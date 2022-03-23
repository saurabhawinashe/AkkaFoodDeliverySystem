package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import net.minidev.json.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


import java.util.*;

public class DeliveryApp {


  public final static class Order {
	public final int custId;
	public final int restId;
	public final int itemId;
	public final int qty;
	@JsonCreator
	public Order(@JsonProperty("restId") int restId, @JsonProperty("itemId") int itemId, @JsonProperty("custId") int custId, @JsonProperty("qty") int qty) {
	  this.restId = restId;
	  this.itemId = itemId;
	  this.qty = qty;
	  this.custId = custId;
	}
  }



  public final static class OrderSend implements DeliveryCommand {
	public final int orderId;
	public final int custId;
	public final int restId;
	public final int itemId;
	public final int qty;
	public final String status;
	@JsonCreator
	public OrderSend(@JsonProperty("orderId") int orderId, @JsonProperty("restId") int restId, @JsonProperty("itemId") int itemId, @JsonProperty("custId") int custId, @JsonProperty("qty") int qty, @JsonProperty("status") String status) {
	  this.orderId = orderId;
	  this.restId = restId;
	  this.itemId = itemId;
	  this.qty = qty;
	  this.custId = custId;
	  this.status = status;
	}
  }

  interface DeliveryCommand {}

  public final static class GetOrder implements DeliveryCommand {
	public final int orderId;
	public final ActorRef<DeliveryCommand> replyTo;

	public GetOrder(int orderId, ActorRef<DeliveryCommand> replyTo) {
	  this.replyTo = replyTo;
	  this.orderId = orderId;

	}

  }

  public final static class RequestOrder implements DeliveryCommand {
	public final Order order;
	public final ActorRef<DeliveryCommand> replyTo;

	public RequestOrder(Order order, ActorRef<DeliveryCommand> replyTo) {
	  this.replyTo = replyTo;
	  this.order = order;

	}
  }


  public final static class OrderGen implements DeliveryCommand {
	public final int orderId;
	OrderGen(int orderId){
	  this.orderId = orderId;
	}
  }

  public final static class ReInitialize implements DeliveryCommand {
  	public final ActorRef<DeliveryCommand> replyTo;

  	public ReInitialize(ActorRef<DeliveryCommand> replyTo) {
  		this.replyTo = replyTo;
  	}
  }


  public final static class ActionPerformed implements DeliveryCommand {
  	public final String msg;

  	public ActionPerformed(String msg) {
  		this.msg = msg;
  	}
  }


  interface OrderEvent {}

  public final static class FetchOrder implements OrderEvent {
	public final ActorRef<DeliveryCommand> replyTo;

	public FetchOrder(ActorRef<DeliveryCommand> replyTo) {
	  this.replyTo = replyTo;
	}
  }

  public final static class NewOrder implements OrderEvent {
	
  }


  public static Behavior<DeliveryCommand> create() {
	return Behaviors.setup(Delivery::new);
  }


  public static class Delivery extends AbstractBehavior<DeliveryCommand> {

  private final HashMap<Integer, ActorRef<OrderEvent>> orders = new HashMap<>();
  private int orderId;

  public Delivery(ActorContext<DeliveryCommand> context) {
	super(context);
	this.orderId = 1000;
  }
  


  @Override
  public Receive<DeliveryCommand> createReceive() {
	return newReceiveBuilder()
		.onMessage(GetOrder.class, this::onGetOrder)
		.onMessage(RequestOrder.class, this::onRequestOrder)
		.onMessage(ReInitialize.class, this::onReInitialize)
		.build();
  }

  public Behavior<DeliveryCommand> onGetOrder(GetOrder command) {
	ActorRef<OrderEvent> orderActor = orders.get(command.orderId);
	orderActor.tell(new FetchOrder(command.replyTo));
	return this;
  }


  public Behavior<DeliveryCommand> onRequestOrder(RequestOrder command) {
	ActorRef<OrderEvent> orderActor = getContext().spawn(FulfillOrder.create(getContext().getSelf(), orderId, command.order.restId, command.order.itemId, command.order.qty, command.order.custId, "unassigned"), "FulfillOrder-" + orderId);
	orders.put(orderId, orderActor);
	orderActor.tell(new NewOrder());
	System.out.println("order done");
	command.replyTo.tell(new OrderGen(this.orderId));
	orderId++;
	return this;
  }


  public Behavior<DeliveryCommand> onReInitialize(ReInitialize command) {
  	for (Map.Entry mapElement : orders.entrySet()) {
  		ActorRef<OrderEvent> orderActor = (ActorRef<OrderEvent>)mapElement.getValue();
  		getContext().stop(orderActor);
  	}
  	orderId = 1000;
  	orders.clear();
  	command.replyTo.tell(new ActionPerformed("Reinitialized"));
	return this;
  }

}



public static class FulfillOrder extends AbstractBehavior<OrderEvent> {
  
  public final int orderId;
  public String status;
  public final int custId;
  public final int restId;
  public final int itemId;
  public final int qty;
  public HashMap<Integer, HashMap<Integer, Integer> > restaurants = new HashMap<>();

  private FulfillOrder(ActorContext<OrderEvent> context, int orderId, int restId, int itemId, int qty, int custId, String status) {
	super(context);
	this.orderId = orderId;
	this.restId = restId;
	this.itemId = itemId;
	this.qty = qty;
	this.custId = custId;
	this.status = status;
  }


  public int totalBill() {
	HashMap<Integer, Integer> items = this.restaurants.get(this.restId);

	int price = items.get(this.itemId);

	return price*this.qty;
  }

  public void initRestaurants() throws IOException {
	
	try {
	  File file = new File("initialData.txt");
	
	  Scanner sc = new Scanner(file);
	  
	  int count = -1, restId = -1;
	  HashMap<Integer, Integer> items = new HashMap<>();
	  while(sc.hasNextLine())
	  {
		String line = sc.nextLine();
		line = line.strip();
		if(line.contains("****"))
		  break;
		if(count==-1) {
		  restId = Integer.parseInt(line.split(" ")[0]);
		  count = Integer.parseInt(line.split(" ")[1]);
		  if(count==0)
			count=-1;
		}
		else if(count>0) {
		  items.put(Integer.parseInt(line.split(" ")[0]), Integer.parseInt(line.split(" ")[2]));
		  count--;
		  if(count==0) {
			this.restaurants.put(restId, items);
			restId = -1;
			items = new HashMap<>();
			count = -1;
		  }   
		}
	  }
	  sc.close();
	}
	catch(FileNotFoundException e) {
	  System.out.println("Initialialization file not found!");
	}
	catch(NumberFormatException e) {
	  e.printStackTrace();
	}
  }

  public static Behavior<OrderEvent> create(ActorRef<DeliveryCommand> delivery, int orderId, int restId, int itemId, int qty, int custId, String status) {
	return Behaviors.setup(context -> new FulfillOrder(context, orderId, restId, itemId, qty, custId, status));

  }

  

  

  @Override
  public Receive<OrderEvent> createReceive() {
	return newReceiveBuilder()
		.onMessage(NewOrder.class, this::onNewOrder)
		.onMessage(FetchOrder.class, this::onFetchOrder)
		//.onMessage(Delete.class, this::onDelete)
		.build();
  }

  public Behavior<OrderEvent> onNewOrder(NewOrder command) throws IOException{
	this.initRestaurants();
	RestTemplate restTemplate = new RestTemplate();
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	JSONObject entityWallet = new JSONObject();
	entityWallet.appendField("custId", this.custId);
	entityWallet.appendField("amount", this.totalBill());
	HttpEntity<Object> httpEntityWallet = new HttpEntity<Object>(entityWallet.toString(), headers);
	try {
		HttpStatus responseWallet = restTemplate.exchange("http://localhost:8082/deductBalance", HttpMethod.POST, httpEntityWallet, Object.class).getStatusCode();
		if(responseWallet == HttpStatus.CREATED) {
		  String url = "http://localhost:8080/acceptOrder";
		  JSONObject entityRestaurant = new JSONObject();
		  entityRestaurant.appendField("restId", restId);
		  entityRestaurant.appendField("itemId", itemId);
		  entityRestaurant.appendField("qty", qty);

		  HttpEntity<String> httpEntityRestaurant = new HttpEntity<String>(entityRestaurant.toString(), headers);
		  HttpStatus responseRestaurant = restTemplate.exchange(url, HttpMethod.POST, httpEntityRestaurant, String.class).getStatusCode();

		  if(responseRestaurant == HttpStatus.CREATED) {
			this.status = "delivered";
		  }
		}	
	}
	catch(HttpClientErrorException e) {
		httpEntityWallet = new HttpEntity<Object>(entityWallet.toString(), headers);
		restTemplate.exchange("http://localhost:8082/addBalance", HttpMethod.POST, httpEntityWallet, Object.class);
	}
	
	return this;
  }


  public Behavior<OrderEvent> onFetchOrder(FetchOrder command) {
  	System.out.println("command");	
	command.replyTo.tell(new OrderSend(this.orderId, this.restId, this.itemId, this.custId, this.qty, this.status));
	return this;
  }

}
}