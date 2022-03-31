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
	public final String status;
	@JsonCreator
	public OrderSend(@JsonProperty("orderId") int orderId, @JsonProperty("status") String status) {
	  this.orderId = orderId;
	  this.status = status;
	}
  }

  interface DeliveryCommand {}

  public final static class GetOrder implements DeliveryCommand {
	public final int orderId;
	public final ActorRef<OrderSend> replyTo;

	public GetOrder(int orderId, ActorRef<OrderSend> replyTo) {
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
	public final ActorRef<OrderSend> replyTo;

	public FetchOrder(ActorRef<OrderSend> replyTo) {
	  this.replyTo = replyTo;
	}
  }

  public final static class NewOrder implements OrderEvent {
	
  }


  public static Behavior<DeliveryCommand> create() {
	return Behaviors.setup(Delivery::new);
  }

  /**
   * The Delivery actor has the following messages : 
   * 		1. GetOrder : The client sends this message to receive the details of the order
   * 		2. RequestOrder : The client uses this message to create an order
   * 		3. ReInitialize : The client sends this message to reinitialize the Delivery actor and its associated orders
   * 
   * The Delivery actor has the global order ID that it uses to initialie FulfillOrder actors. On reinitializing, this global order ID is also set to its default value.
   * It also has a list of spawned Orders in the form of a HashMap.
  */
  public static class Delivery extends AbstractBehavior<DeliveryCommand> {

  private final HashMap<Integer, ActorRef<OrderEvent>> orders = new HashMap<>();
  private int orderId;
  private int spawnId;

  public Delivery(ActorContext<DeliveryCommand> context) {
	super(context);
	this.orderId = 1000;
	this.spawnId = 0;
  }
  


  @Override
  public Receive<DeliveryCommand> createReceive() {
	return newReceiveBuilder()
		.onMessage(GetOrder.class, this::onGetOrder)
		.onMessage(RequestOrder.class, this::onRequestOrder)
		.onMessage(ReInitialize.class, this::onReInitialize)
		.build();
  }

  /**
   * This method is executed on receiving a GetOrder message from the client along with an order ID.
   * It forwards the request to the corresponding FulfillOrder actor using the FetchOrder message.
   * If the order ID is invalid, it sends an invalid Order object back which is then translated to a 404.
  **/
  public Behavior<DeliveryCommand> onGetOrder(GetOrder command) {
	ActorRef<OrderEvent> orderActor = orders.get(command.orderId);
	if(orderActor != null)
		orderActor.tell(new FetchOrder(command.replyTo));
	else
		command.replyTo.tell(new OrderSend(-1, "NA"));
	return this;
  }

  /**
   * This method is executed on receiving a RequestOrder message from the client.
   * It executes the following steps :
   * 		1. Spawn a new FulfillOrder actor with the current global order ID and spawn ID value.
   * 		2. Add the ActorRef to the orders HashMap with its key as the global order ID value.
   * 		3. Send a message to the newly spawned FulfillOrder actor. The message to be sent is NewOrder.
   * 		4. Send a reply back to the client with the order ID of the newly spawned order.
   * 		5. Increment the global order ID and the spawn ID.
  **/
  public Behavior<DeliveryCommand> onRequestOrder(RequestOrder command) {
	ActorRef<OrderEvent> orderActor = getContext().spawn(FulfillOrder.create(getContext().getSelf(), orderId, command.order.restId, command.order.itemId, command.order.qty, command.order.custId, "unassigned"), "FulfillOrder-" + orderId + "-" + spawnId);
	orders.put(orderId, orderActor);
	orderActor.tell(new NewOrder());
	System.out.println("order done");
	command.replyTo.tell(new OrderGen(this.orderId));
	orderId++;
	spawnId++;
	return this;
  }

  /**
   * This method is executed on receiving a ReInitialize message from the client.
   * It performs the following steps : 
   * 		1. Loop through all the ActorRefs present in the orders HashMap and stop them.
   * 		2. Set the global order ID back to its default value.
   * 		3. Clear the orders HashMap
   * 		4. Send a reply back to the client.
	**/
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


/**
 * The FulfillOrder actor corresponds to a order in the system. Each FulfillOrder actor can receive the following messages : 
 * 		1. NewOrder : The Delivery actor sends this message on spawning a new order.
 * 		2. FetchOrder : The Delivery actor sends this message to instruct the Order actor to send the order details to the client.
 * 
 * The Fulfill Order has in its state all the necessary details about the order ie, orderID, status, restId, itemId, custId and the qty.
*/
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

  /**
   * This method computes the total bill associated with the order.
  **/
  public int totalBill() {
	HashMap<Integer, Integer> items = this.restaurants.get(this.restId);

	int price = items.get(this.itemId);

	System.out.println("Val : " + price);

	return price*this.qty;
  }

  /**
   * This method reads data from the initialData.txt file and create a structure that stores the restaurant ID along with its associated items and their prices.
  **/
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
		  items.put(Integer.parseInt(line.split(" ")[0]), Integer.parseInt(line.split(" ")[1]));
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

  /**
   * This method is executed on receiving a NewOrder message from the Delivery actor.
   * It performs the following steps : 
   * 		1. Initialize the restaurants from the initialData.txt file.
   * 		2. Compute the total bill for the order.
   * 		3. Deduct the customer's balance.
   * 		4. If the balance was successfully deducted, update the inventory.
   * 		5. If the inventory update was successful, mark the order as delivered.
  **/
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
		//httpEntityWallet = new HttpEntity<Object>(entityWallet.toString(), headers);
		//restTemplate.exchange("http://localhost:8082/addBalance", HttpMethod.POST, httpEntityWallet, Object.class);
	}
	
	return this;
  }

  /**
   * This method is executed on receiving a FetchOrder message from the Delivery Actor.
   * It sends the order ID and the status to the client. The Client is a part of the message that was sent by the Delivery actor. 
  **/
  public Behavior<OrderEvent> onFetchOrder(FetchOrder command) {
	command.replyTo.tell(new OrderSend(this.orderId, this.status));
	return this;
  }

}
}