package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.compat.Future;
import akka.pattern.Patterns;
import akka.util.Timeout;

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
import java.time.Duration;
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

  public final static class AgentReceive {
	  public final int agentId;
	  @JsonCreator
		public AgentReceive(@JsonProperty("agentId") int agentId) {
		  this.agentId = agentId;
		}
  }

  public final static class OrderSend implements DeliveryCommand {
	public final int orderId;
	public final String status;
	public final int agentId;
	@JsonCreator
	public OrderSend(@JsonProperty("orderId") int orderId, @JsonProperty("status") String status, @JsonProperty("agentId") int agentId) {
	  this.orderId = orderId; 
	  this.status = status;
	  this.agentId = agentId;
	}
  }
  
  public final static class AgentSend implements DeliveryCommand {
	  public final int agentId;
	  public final String status;
		@JsonCreator
		public AgentSend(@JsonProperty("agentId") int agentId, @JsonProperty("status") String status) {
		  this.agentId = agentId;
		  this.status = status;
		}
  }
  
  public final static class AgentSendOrder implements DeliveryCommand {
	  public final int agentId;
	  public final String status;
		@JsonCreator
		public AgentSendOrder(@JsonProperty("agentId") int agentId, @JsonProperty("status") String status) {
		  this.agentId = agentId;
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
  
  public final static class GetAgentStatus implements DeliveryCommand {
	  public final ActorRef<AgentSend> replyTo;
	  public final int agentId;
	  
	  public GetAgentStatus(int agentId, ActorRef<AgentSend> replyTo) {
		  this.replyTo = replyTo;
		  this.agentId = agentId;
		}
  }
  
  public final static class AgentSignIn implements DeliveryCommand {
	  public final ActorRef<DeliveryCommand> replyTo;
	  public final AgentReceive agent;
	  
	  public AgentSignIn(AgentReceive agent, ActorRef<DeliveryCommand> replyTo) {
		  this.agent = agent;
		  this.replyTo = replyTo;
	  }
  }
  
  public final static class AgentSignOut implements DeliveryCommand {
	  public final ActorRef<DeliveryCommand> replyTo;
	  public final AgentReceive agent;
	  
	  public AgentSignOut(AgentReceive agent, ActorRef<DeliveryCommand> replyTo) {
		  this.agent = agent;
		  this.replyTo = replyTo;
	  }
  }
  
  public final static class AgentReady implements DeliveryCommand {
	  public final int agentId;
	  
	  public AgentReady(int agentId) {
		  this.agentId = agentId;
	  }
  }
  
  public final static class AgentAssignedtoOrder implements DeliveryCommand {
	  public final int orderId;
	  
	  public AgentAssignedtoOrder(int orderId) {
		  this.orderId = orderId;
	  }
  }
  
  public final static class OrderDelivered implements DeliveryCommand {
	  public final OrderGen order;
	  public final ActorRef<DeliveryCommand> replyTo;
	  
	  public OrderDelivered(OrderGen order, ActorRef<DeliveryCommand> replyTo) {
		  this.order = order;
		  this.replyTo = replyTo;
	  }
  }

  public final static class OrderGen implements DeliveryCommand {
	public final int orderId;
	
	@JsonCreator
	OrderGen(@JsonProperty("orderId") int orderId){
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
	  public final ActorRef<DeliveryCommand> replyTo;

		public NewOrder(ActorRef<DeliveryCommand> replyTo) {
		  this.replyTo = replyTo;
		}
  }
  
  public final static class AgentData implements OrderEvent {
	  public final int agentId;
	  public final String status;
	  
	  public AgentData(int agentId, String status) {
		  this.agentId = agentId;
		  this.status = status;
	  }
  }
  
  public final static class AgentAvailable implements OrderEvent {
	  public final int agentId;
	  public final ActorRef<DeliveryCommand> replyTo;
	  
	  public AgentAvailable(ActorRef<DeliveryCommand> replyTo, int agentId) {
		  this.replyTo = replyTo;
		  this.agentId = agentId;
	  }
  }
  
  public final static class OrderFulfilled implements OrderEvent {
	  public final ActorRef<DeliveryCommand> replyTo;
	  
	  public OrderFulfilled(ActorRef<DeliveryCommand> replyTo) {
		  this.replyTo = replyTo;
	  }
  }
  
  
  interface AgentEvent {}
  

  
  public final static class GetStatus implements AgentEvent {
	  public final ActorRef<AgentSend> replyTo;

		public GetStatus(ActorRef<AgentSend> replyTo) {
		  this.replyTo = replyTo;
		}
  }
  
  public final static class PingStatus implements AgentEvent {
	  public final ActorRef<DeliveryCommand> replyTo;
	  
	  public PingStatus(ActorRef<DeliveryCommand> replyTo) {
		  this.replyTo = replyTo;
	  }
  }
  
  public final static class GetStatusOrder implements AgentEvent {
	  /*public final ActorRef<AgentData> replyTo;

		public GetStatusOrder(ActorRef<AgentData> replyTo) {
		  this.replyTo = replyTo;
		}*/
	  public final ActorRef<OrderEvent> replyTo;
	  
	  public GetStatusOrder(ActorRef<OrderEvent> replyTo) {
		  this.replyTo = replyTo;
	  }
  }
  
  public final static class ChangeStatus implements AgentEvent {
	  public final String status;
	  public final ActorRef<DeliveryCommand> replyTo;
	  
	  public ChangeStatus(String status, ActorRef<DeliveryCommand> replyTo) {
		  this.status = status;
		  this.replyTo = replyTo;
	  }
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
  private final TreeMap<Integer, ActorRef<AgentEvent>> agents = new TreeMap<>();
  private final TreeMap<Integer, ActorRef<OrderEvent>> orders_waiting = new TreeMap<>();
  private int orderId;
  private int spawnId;

  public void initAgents() {
	  try {
		  File file = new File("initialData.txt");
		
		  Scanner sc = new Scanner(file);
		  
		  int agentId, flag=0;
		  while(sc.hasNextLine())
		  {
			String line = sc.nextLine();
			line = line.strip();
			if(line.contains("****")) {
				if(flag==0) {
					flag = 1;
					continue;
				}
				else
					break;
			}
			if(flag==0)
				continue;
			
			else {
			  agentId = Integer.parseInt(line.split(" ")[0]);
			  ActorRef<AgentEvent> agent = getContext().spawn(Agent.create(getContext().getSelf(), agentId, "signed-out"), "Agent-" + agentId);
			  this.agents.put(agentId, agent);
			  
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
  
  public Delivery(ActorContext<DeliveryCommand> context) {
	super(context);
	this.orderId = 1000;
	this.spawnId = 0;
	initAgents();
  }
  


  @Override
  public Receive<DeliveryCommand> createReceive() {
	return newReceiveBuilder()
		.onMessage(GetOrder.class, this::onGetOrder)
		.onMessage(RequestOrder.class, this::onRequestOrder)
		.onMessage(ReInitialize.class, this::onReInitialize)
		.onMessage(GetAgentStatus.class, this::onGetAgentStatus)
		.onMessage(AgentSignIn.class, this::onAgentSignIn)
		.onMessage(AgentSignOut.class, this::onAgentSignOut)
		.onMessage(AgentReady.class, this::onAgentReady)
		.onMessage(AgentAssignedtoOrder.class, this::onAgentAssignedtoOrder)
		.onMessage(OrderDelivered.class, this::onOrderDelivered)
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
		command.replyTo.tell(new OrderSend(-1, "NA", -1));
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
	ActorRef<OrderEvent> orderActor = getContext().spawn(FulfillOrder.create(getContext().getSelf(), orderId, command.order.restId, command.order.itemId, command.order.qty, command.order.custId, "unassigned", agents), "FulfillOrder-" + orderId + "-" + spawnId);
	orders.put(orderId, orderActor);
	orders_waiting.put(orderId, orderActor);
	orderActor.tell(new NewOrder(getContext().getSelf()));
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
  	orders_waiting.clear();
  	for (Map.Entry mapElement : agents.entrySet()) {
  		ActorRef<AgentEvent> agent = (ActorRef<AgentEvent>)mapElement.getValue();
  		agent.tell(new ChangeStatus("signed-out", null));
  	}
  	command.replyTo.tell(new ActionPerformed("Reinitialized"));
	return this;
  }
  
  public Behavior<DeliveryCommand> onGetAgentStatus(GetAgentStatus command) {
	  	ActorRef<AgentEvent> agent = agents.get(command.agentId);
	  	agent.tell(new GetStatus(command.replyTo));
		return this;
	  }
  
  public Behavior<DeliveryCommand> onAgentSignIn(AgentSignIn command) {
	  	ActorRef<AgentEvent> agent = agents.get(command.agent.agentId);
	  	agent.tell(new ChangeStatus("available-1", getContext().getSelf()));
	  	command.replyTo.tell(new ActionPerformed("Status changed"));
		return this;
	  }
  
  public Behavior<DeliveryCommand> onAgentSignOut(AgentSignOut command) {
	  	ActorRef<AgentEvent> agent = agents.get(command.agent.agentId);
	  	agent.tell(new ChangeStatus("signed-out", getContext().getSelf()));
	  	command.replyTo.tell(new ActionPerformed("Status changed"));
		return this;
	  }
  
  public Behavior<DeliveryCommand> onAgentReady(AgentReady command) {
	  	System.out.println("agent : " + command.agentId + "changed");
	  	if(orders_waiting.size() > 0) {
	  		int orderId = orders_waiting.firstKey();
		  	ActorRef<OrderEvent> orderActor = orders_waiting.get(orderId);
		  	orderActor.tell(new AgentAvailable(getContext().getSelf(), command.agentId));
	  	}
		return this;
	  }
  
  public Behavior<DeliveryCommand> onAgentAssignedtoOrder(AgentAssignedtoOrder command) {
	  	orders_waiting.remove(command.orderId);	
	  	//System.out.println("agent : " + command.agentId + "changed");
		return this;
	  }
  
  public Behavior<DeliveryCommand> onOrderDelivered(OrderDelivered command) {
	  	ActorRef<OrderEvent> orderActor = orders.get(command.order.orderId);
	  	orderActor.tell(new OrderFulfilled(getContext().getSelf()));
	  	command.replyTo.tell(new ActionPerformed("Done"));
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
  public int agentId;
  public HashMap<Integer, HashMap<Integer, Integer> > restaurants = new HashMap<>();
  public TreeMap<Integer, ActorRef<AgentEvent>> agents;

  private FulfillOrder(ActorContext<OrderEvent> context, int orderId, int restId, int itemId, int qty, int custId, String status, TreeMap<Integer, ActorRef<AgentEvent>> agents) {
	super(context);
	this.orderId = orderId;
	this.restId = restId;
	this.itemId = itemId;
	this.qty = qty;
	this.custId = custId;
	this.agentId = -1;
	this.status = status;
	this.agents = agents;
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

  public static Behavior<OrderEvent> create(ActorRef<DeliveryCommand> delivery, int orderId, int restId, int itemId, int qty, int custId, String status, TreeMap<Integer, ActorRef<AgentEvent>> agents) {
	return Behaviors.setup(context -> new FulfillOrder(context, orderId, restId, itemId, qty, custId, status, agents));

  }

  

  

  @Override
  public Receive<OrderEvent> createReceive() {
	return newReceiveBuilder()
		.onMessage(NewOrder.class, this::onNewOrder)
		.onMessage(FetchOrder.class, this::onFetchOrder)
		.onMessage(AgentAvailable.class, this::onAgentAvailable)
		.onMessage(AgentData.class, this::onAgentData)
		.onMessage(OrderFulfilled.class, this::onOrderFulfilled)
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
			System.out.println("reached");
		  String url = "http://localhost:8080/acceptOrder";
		  JSONObject entityRestaurant = new JSONObject();
		  entityRestaurant.appendField("restId", restId);
		  entityRestaurant.appendField("itemId", itemId);
		  entityRestaurant.appendField("qty", qty);

		  HttpEntity<String> httpEntityRestaurant = new HttpEntity<String>(entityRestaurant.toString(), headers);
		  try {
			  HttpStatus responseRestaurant = restTemplate.exchange(url, HttpMethod.POST, httpEntityRestaurant, String.class).getStatusCode();
		  }
		  catch(HttpClientErrorException e) {
			  status = "rejected";
			  httpEntityWallet = new HttpEntity<Object>(entityWallet.toString(), headers);
			  restTemplate.exchange("http://localhost:8082/addBalance", HttpMethod.POST, httpEntityWallet, Object.class);
			  return this;
		  }
		}
	}
	catch(HttpClientErrorException e) {
		status = "rejected";
		return this;
	}
	final Duration timeout = Duration.ofSeconds(3);
	
	
	
	
	//TreeMap<Integer, ActorRef<AgentEvent>> av_agents = new TreeMap<>();
	ActorRef<AgentEvent> agent;
	for (Map.Entry mapElement : agents.entrySet()) {
  		agent = (ActorRef<AgentEvent>)mapElement.getValue();
  		System.out.println((Integer)mapElement.getKey());
  		
  		//Future<Object> future =  Patterns.ask(agent, new GetStatusOrder(getContext().getSelf()), timeout);
  		
  		/*getContext().ask(AgentData.class, agent, timeout, (ActorRef<AgentData> ref) -> new GetStatusOrder(ref), (response, throwable)-> {
  			if(response!=null) {
  				if(response.status.equals("available")) {
  					//status = "assigned";
  					agentId = (Integer)mapElement.getKey();
  					System.out.println("new:"+agentId);
  					ActorRef<AgentEvent> agentActor = (ActorRef<AgentEvent>)mapElement.getValue();
  					//agentActor.tell(new ChangeStatus("unavailable", null));
  					//command.replyTo.tell(new AgentAssignedtoOrder(orderId));
  					av_agents.put(agentId, agentActor);
  				}
  			}
  			else
  				System.out.println("error");
  			return null;
  		});*/
  		/*if(status.equals("assigned"))
  			break;*/
  		
  	}
	return this;
  }

  /**
   * This method is executed on receiving a FetchOrder message from the Delivery Actor.
   * It sends the order ID and the status to the client. The Client is a part of the message that was sent by the Delivery actor. 
  **/
  public Behavior<OrderEvent> onFetchOrder(FetchOrder command) {
	command.replyTo.tell(new OrderSend(this.orderId, this.status, this.agentId));
	return this;
  }
  
  
  public Behavior<OrderEvent> onAgentAvailable(AgentAvailable command) {
	  	if(status.equals("rejected"))
	  		return this;
	  	final Duration timeout = Duration.ofSeconds(3);
	  	ActorRef<AgentEvent> agent = agents.get(command.agentId);
	  	if(status.equals("assigned"))
	  		agent.tell(new PingStatus(command.replyTo));
		/*getContext().ask(AgentData.class, agent, timeout, (ActorRef<AgentData> ref) -> new GetStatusOrder(ref), (response, throwable)-> {
			if(response!=null) {
				if(response.status.equals("available")) {
					status = "assigned";
					agentId = command.agentId;
					ActorRef<AgentEvent> agentActor = agents.get(command.agentId);
					agentActor.tell(new ChangeStatus("unavailable", null));
					command.replyTo.tell(new AgentAssignedtoOrder(orderId));
				}
			}
			else
				System.out.println("error");
			return null;
		});*/
		return this;
	  }
  
  public Behavior<OrderEvent> onAgentData(AgentData command) {
		//command.replyTo.tell(new OrderSend(this.orderId, this.status));
		return this;
	  }
  
  public Behavior<OrderEvent> onOrderFulfilled(OrderFulfilled command) {
	  	if(agentId != -1) {
	  		status = "delivered";
		  	ActorRef<AgentEvent> agent = agents.get(agentId);
		  	agent.tell(new ChangeStatus("available-2", command.replyTo));
			//command.replyTo.tell(new OrderSend(this.orderId, this.status));
	  	}
		return this;
	  }

}

public static class Agent extends AbstractBehavior<AgentEvent> {
	
	public final int agentId;
	public String status;
	
	private Agent(ActorContext<AgentEvent> context, int agentId, String status) {
		super(context);
		this.agentId = agentId;
		this.status = status;
	  }
	
	public static Behavior<AgentEvent> create(ActorRef<DeliveryCommand> delivery, int agentId, String status) {
		return Behaviors.setup(context -> new Agent(context, agentId, status));

	  }
	
	@Override
	  public Receive<AgentEvent> createReceive() {
		return newReceiveBuilder()
			.onMessage(GetStatus.class, this::onGetStatus)
			.onMessage(GetStatusOrder.class, this::onGetStatusOrder)
			.onMessage(ChangeStatus.class, this::onChangeStatus)
			.onMessage(PingStatus.class, this::onPingStatus)
			//.onMessage(Delete.class, this::onDelete)
			.build();
	  }
	
	public Behavior<AgentEvent> onGetStatus(GetStatus command) {
		command.replyTo.tell(new AgentSend(agentId, status));
		return this;
	  }
	
	public Behavior<AgentEvent> onGetStatusOrder(GetStatusOrder command) {
		command.replyTo.tell(new AgentData(agentId, status));
		return this;
	  }
	
	public Behavior<AgentEvent> onChangeStatus(ChangeStatus command) {
		String tempStatus = command.status;
		if(status.equals("unavailable") && command.status.equals("signed-out"))
			return this;
		if(!status.equals("available") && command.status.equals("unavailable"))
			return this;
		if(status.equals("signed-out") && command.status.equals("available-1")) {
			command.replyTo.tell(new AgentReady(agentId));
			tempStatus = "available";
		}
		if(status.equals("unavailable") && command.status.equals("available-2")) {
			command.replyTo.tell(new AgentReady(agentId));
			tempStatus = "available";
		}
			
		status = tempStatus;;
		return this;
	  }
	
	public Behavior<AgentEvent> onPingStatus(PingStatus command) {
		if(status.equals("available"))
			command.replyTo.tell(new AgentReady(agentId));
		return this;
	  }
	
}
}