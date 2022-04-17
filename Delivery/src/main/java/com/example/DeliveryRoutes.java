package com.example;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.example.DeliveryApp.AgentReceive;
import com.example.DeliveryApp.Order;
import com.example.DeliveryApp.OrderGen;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;

import static akka.http.javadsl.server.Directives.*;

import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static akka.http.javadsl.server.PathMatchers.integerSegment;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import java.lang.Thread;


public class DeliveryRoutes {

    private final static Logger log = LoggerFactory.getLogger(DeliveryRoutes.class);
    private final ActorRef<DeliveryApp.DeliveryCommand> deliveryActor;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public DeliveryRoutes(ActorSystem<?> system, ActorRef<DeliveryApp.DeliveryCommand> deliveryActor) {
        this.deliveryActor = deliveryActor;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("my-app.routes.ask-timeout");
    }

    private CompletionStage<DeliveryApp.DeliveryCommand> requestOrder(Order order) {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.RequestOrder(order, ref), askTimeout, scheduler);
    }

    private CompletionStage<DeliveryApp.OrderSend> getOrder(int orderId) {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.GetOrder(orderId, ref), askTimeout, scheduler);
    }
    
    private CompletionStage<DeliveryApp.DeliveryCommand> deliverOrder(OrderGen order) {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.OrderDelivered(order, ref), askTimeout, scheduler);
    }
    
    private CompletionStage<DeliveryApp.AgentSend> getAgent(int agentId) {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.GetAgentStatus(agentId, ref), askTimeout, scheduler);
    }
    
    private CompletionStage<DeliveryApp.DeliveryCommand> agentSignIn(AgentReceive agent) {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.AgentSignIn(agent, ref), askTimeout, scheduler);
    }
    
    private CompletionStage<DeliveryApp.DeliveryCommand> agentSignOut(AgentReceive agent) {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.AgentSignOut(agent, ref), askTimeout, scheduler);
    }

    private CompletionStage<DeliveryApp.DeliveryCommand> reInitialize() {
        return AskPattern.ask(deliveryActor, ref -> new DeliveryApp.ReInitialize(ref), askTimeout, scheduler);
    }


    public Route OrderRoutes() {
        return concat(
                    path("requestOrder", () ->
                        pathEnd(() ->
                        post(() ->
                        entity(
                            Jackson.unmarshaller(Order.class),
                            order ->
                                onSuccess(requestOrder(order), performed -> {
                                  log.info("Create result:");
                                  /*try {
                                    Thread.sleep(1000);  
                                  }
                                  catch(Exception e) {
                                    e.printStackTrace();
                                  }*/
                                  return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                                })
                        )
                        )
                    )
                    ),
                    
                    path("orderDelivered", () ->
                    pathEnd(() ->
                    post(() ->
                    entity(
                        Jackson.unmarshaller(OrderGen.class),
                        order ->
                            onSuccess(deliverOrder(order), performed -> {
                              log.info("Create result:");
                              /*try {
                                Thread.sleep(1000);  
                              }
                              catch(Exception e) {
                                e.printStackTrace();
                              }*/
                              return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                            })
                    )
                    )
                )
                ),
                    
                    path("agentSignIn", () ->
                    pathEnd(() ->
                    post(() ->
                    entity(
                        Jackson.unmarshaller(AgentReceive.class),
                        agent ->
                            onSuccess(agentSignIn(agent), performed -> {
                              log.info("Create result:");
                              /*try {
                                Thread.sleep(1000);  
                              }
                              catch(Exception e) {
                                e.printStackTrace();
                              }*/
                              return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                            })
                    )
                    )
                )
                ),
                    
                    path("agentSignOut", () ->
                    pathEnd(() ->
                    post(() ->
                    entity(
                        Jackson.unmarshaller(AgentReceive.class),
                        agent ->
                            onSuccess(agentSignOut(agent), performed -> {
                              log.info("Create result:");
                              /*try {
                                Thread.sleep(1000);  
                              }
                              catch(Exception e) {
                                e.printStackTrace();
                              }*/
                              return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                            })
                    )
                    )
                )
                ),

                    pathPrefix("order", () -> 
                        path(integerSegment(), (Integer orderId) ->
                        concat(
                            get(() -> 
                                onSuccess(getOrder(orderId), performed -> {
                                        /*try {
                                            Thread.sleep(1000);  
                                        }
                                        catch(Exception e) {
                                            e.printStackTrace();
                                        }*/
                                        log.info(performed.status);
                                        if(performed.status.equals("NA"))
                                            return complete(StatusCodes.NOT_FOUND, new JSONObject(),Jackson.marshaller());
                                        else
                                            return complete(StatusCodes.OK, performed, Jackson.marshaller());
                                    
                                }
                                
                                )
                            )
                        )
                    )
                    ),
                    
                    pathPrefix("agent", () -> 
                    path(integerSegment(), (Integer agentId) ->
                    concat(
                        get(() -> 
                            onSuccess(getAgent(agentId), performed -> {
                                    /*try {
                                        Thread.sleep(1000);  
                                    }
                                    catch(Exception e) {
                                        e.printStackTrace();
                                    }*/
                                    log.info(performed.status);
                                    if(performed.status.equals("NA"))
                                        return complete(StatusCodes.NOT_FOUND, new JSONObject(),Jackson.marshaller());
                                    else
                                        return complete(StatusCodes.OK, performed, Jackson.marshaller());
                                
                            }
                            
                            )
                        )
                    )
                )
                ),
                    
                    

                    path("reInitialize", () ->
                        pathEnd(() ->
                        post(() ->
                                onSuccess(reInitialize(), performed -> {
                                  JSONObject entity = new JSONObject();
                                  log.info("Reinitialized");
                                  /*try {
                                    Thread.sleep(1000);  
                                  }
                                  catch(Exception e) {
                                    e.printStackTrace();
                                  } */
                                  return complete(StatusCodes.CREATED, entity, Jackson.marshaller());
                                })
                        )
                        )
                    )

                ) ;
    }

}