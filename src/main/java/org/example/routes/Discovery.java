package org.example.routes;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.example.database.DiscoveryQuery;
import org.example.utils.Config;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.example.Bootstrap.client;
import static org.example.Bootstrap.vertx;

public class Discovery
{
    private final DiscoveryQuery discoveryQuery = new DiscoveryQuery(client);

    public void route(Router discoveryRouter)
    {
        try
        {
            discoveryRouter.post("/create")
                    .handler(TimeoutHandler.create(5000))
                    .handler(BodyHandler.create())
                    .handler(this::create);

            discoveryRouter.put("/update")
                    .handler(TimeoutHandler.create(5000))
                    .handler(BodyHandler.create())
                    .handler(this::update);

            discoveryRouter.get("/getAll")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::getAll);

            discoveryRouter.get("/get")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::get);

            discoveryRouter.delete("/delete")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::delete);

            discoveryRouter.post("/:id/run")
                            .handler(TimeoutHandler.create(5000))
                                    .handler(this::discover);

            discoveryRouter.get("/notfound").handler(ctx->{
                ctx.response().setStatusCode(500).end("No such request");
            });

            discoveryRouter.route().failureHandler(ctx->{
                if(ctx.statusCode()==500)
                {
                    ctx.reroute("/notfound");
                }
                else
                {
                    ctx.next();
                }
            });
        }
        catch (Exception exception)
        {
            System.out.println("Error in discovery routing : "+exception.getMessage());
        }
    }

    //Creating discovery

    private void create(RoutingContext context)
    {
        try
        {
            // Parse the incoming JSON payload
            var requestBody = context.body().asJsonObject();

            var discoveryName = requestBody.getString("discovery.name");

            var ip = requestBody.getString("discovery.ip");

            var port = requestBody.getInteger("discovery.port");

            Optional<String> error = Config.validateFields(discoveryName,ip,port);

            if(error.isPresent())
            {
                Config.respond(context,400,error.get());

                return;
            }

            // Process and respond
            var discoveryID = System.currentTimeMillis();
            JsonObject responseJson = new JsonObject()
                    .put("discoveryName", discoveryName)
                    .put("ip", ip)
                    .put("port", port)
                    .put("discoveryID", discoveryID);

            // Insert into the database
            discoveryQuery.insert(discoveryName,ip,port,discoveryID)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            context.response()
                                    .putHeader("content-type","application/json")
                                    .end(responseJson.encodePrettily());
                        }
                        else
                        {
                            context.response().setStatusCode(400).end(result.cause().getMessage());
                        }
                    });
        }
        catch (Exception e)
        {
            context.response()
                    .setStatusCode(500)
                    .end("Server error: " + e.getMessage());
        }
    }

    //Fetching all discoveries from table

    private void getAll(RoutingContext context)
    {
        try
        {
            discoveryQuery.getAllDiscoveries()
                    .onComplete(result->{
                       if(result.succeeded())
                       {
                           context.response()
                                   .putHeader("content-type","application/json")
                                   .end(result.result().encodePrettily());
                       }
                       else
                       {
                           context.response().setStatusCode(400).end(result.cause().getMessage());
                       }
                    });
        }
        catch (Exception exception)
        {
            System.out.println("Error in fetching discoveries : "+exception.getMessage());
            context.response()
                    .setStatusCode(500)
                    .end("Server error: unable to process request");
        }
    }

    private void get(RoutingContext context)
    {
        try
        {
            var longID = context.queryParam("discovery.id").get(0);

            if(longID == null || longID.isEmpty())
            {
                context.response()
                        .setStatusCode(400)
                        .end("Missing on invalid long id parameter");
                return;
            }
            try
            {
                var discoveryID = Long.parseLong(longID);

                discoveryQuery.getDiscovery(discoveryID)
                        .onComplete(result->{
                            if(result.succeeded())
                            {
                                context.response()
                                        .putHeader("content-type","application/json")
                                        .end(result.result().encodePrettily());
                            }
                            else
                            {
                                context.response().setStatusCode(400).end(result.cause().getMessage());
                            }
                        });
            }
            catch (NumberFormatException exception)
            {
                context.response()
                        .setStatusCode(500)
                        .end("Invalid format. It must be a long ID");
            }
        }
        catch (Exception exception)
        {
            System.out.println("Error in fetching discovery of this ID : "+exception.getMessage());
            context.response()
                    .setStatusCode(500)
                    .end("Server error: unable to process request");
        }
    }

    //deleting discovery

    private void delete(RoutingContext context)
    {
        try
        {
            var longID = context.queryParam("discovery.id").get(0);

            if(longID == null || longID.isEmpty())
            {
                context.response()
                        .setStatusCode(400)
                        .end("Missing on invalid long id parameter");
                return;
            }
            try
            {
                var discoveryID = Long.parseLong(longID);

                discoveryQuery.deleteDiscovery(discoveryID)
                        .onComplete(result->{
                            if(result.succeeded())
                            {
                                context.response().setStatusCode(200).end("Discovery successfully deleted");
                            }
                            else
                            {
                                context.response().setStatusCode(400).end(result.cause().getMessage());
                            }
                        });
            }
            catch (NumberFormatException exception)
            {
                context.response()
                        .setStatusCode(500)
                        .end("Invalid format. It must be a long ID");
            }
        }
        catch (Exception exception)
        {
            System.out.println("Error in deleting discovery of this ID : "+exception.getMessage());
            context.response()
                    .setStatusCode(500)
                    .end("Server error: unable to process request");
        }
    }

    //Updating discovery

    private void update(RoutingContext context)
    {
        try
        {
            var requestBody = context.body().asJsonObject();

            var discoveryID = requestBody.getLong("discovery.id");

            var discoveryName = requestBody.getString("discovery.name");

            var ip = requestBody.getString("discovery.ip");

            var port = requestBody.getInteger("discovery.port");

            if(discoveryID == null || discoveryID < 0)
            {
                context.response()
                        .setStatusCode(400)
                        .end("Invalid or missing discovery ID");
                return;
            }

            Optional<String> error = Config.validateFields(discoveryName,ip,port);

            if(error.isPresent())
            {
                Config.respond(context,400,error.get());

                return;
            }

            discoveryQuery.updateDiscovery(discoveryName,ip,port,discoveryID)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            context.response().setStatusCode(200).end("Discovery successfully updated");
                        }
                        else
                        {
                            context.response().setStatusCode(400).end(result.cause().getMessage());
                        }
                    });

        }
        catch (Exception exception)
        {
            System.out.println("Error in updating discovery of this ID : "+exception.getMessage());
            context.response()
                    .setStatusCode(500)
                    .end("Server error: unable to process request");
        }

    }

    //Checking if device is up
    private void discover(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            System.out.println(discoveryID);

            discoveryQuery.fetch(discoveryID)
                    .compose(result->{
                        var ip = result.getString("ip_address");

                        var port = result.getInteger("port_number");

                        return vertx.<JsonObject>executeBlocking(pingFuture->{
                           try
                           {
                               var status = Config.ping(ip);

                               if(status)
                               {
                                   pingFuture.complete(new JsonObject().put("ip",ip).put("port",port));
                               }
                               else
                               {
                                   pingFuture.fail("Device is down, ping failed");
                               }

                           }
                           catch (Exception exception)
                           {
                               pingFuture.fail("Error during ping: "+exception.getMessage());
                           }
                        });
                    })
                    .compose(deviceInfo->{
                        var ip = deviceInfo.getString("ip");

                        var port = deviceInfo.getInteger("port");

                        try
                        {
                            if (Config.isPortOpen(ip, port))
                            {
                                return Future.succeededFuture(deviceInfo.put("status","UP"));
                            }
                            else
                            {
                                return Future.failedFuture("Device is up but port is closed");
                            }
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("Error during ping: "+exception.getMessage());
                        }
                    })
                    .compose(deviceInfo->{
                        var status = deviceInfo.getString("status");

                        var timestamp = Timestamp.from(Instant.now());

                        return discoveryQuery.updateStatus(discoveryID,status,timestamp)
                                .compose(updateResult -> {
                                    if (updateResult)
                                    {
                                        return Future.succeededFuture("Device is up, port is open, and status updated in database");
                                    }
                                    else {
                                        return Future.failedFuture("Device is up, port is open , but failed to update the database");
                                    }
                                });
                    })
                    .onSuccess(finalResult->{
                        context.response().setStatusCode(200).end(finalResult);
                    })
                    .onFailure(error->{
                        context.response().setStatusCode(400).end(error.getMessage());
                    });
        }
        catch (Exception exception)
        {
            System.out.println("Error in checking status of this ID : "+exception.getMessage());
            context.response()
                    .setStatusCode(500)
                    .end("Server error: unable to process request");
        }
    }
}
