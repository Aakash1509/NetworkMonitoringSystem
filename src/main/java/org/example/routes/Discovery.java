package org.example.routes;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.example.database.DiscoveryQuery;
import org.example.model.Credential;
import org.example.model.Discoveries;
import org.example.utils.ApiResponse;
import org.example.utils.Config;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.example.Bootstrap.vertx;

public class Discovery implements CrudOperations
{
    private final DiscoveryQuery discoveryQuery = new DiscoveryQuery();

    public void route(Router discoveryRouter)
    {
        try
        {
            discoveryRouter.post("/create")
                    .handler(TimeoutHandler.create(5000))
                    .handler(BodyHandler.create())
                    .handler(this::create)
                    .failureHandler(this::timeout);

            discoveryRouter.put("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(BodyHandler.create())
                    .handler(this::update)
                    .failureHandler(this::timeout);

            discoveryRouter.get("/getAll")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::getAll)
                    .failureHandler(this::timeout);

            discoveryRouter.get("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::get)
                    .failureHandler(this::timeout);

            discoveryRouter.delete("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::delete)
                    .failureHandler(this::timeout);

            discoveryRouter.post("/:id/run")
                            .handler(TimeoutHandler.create(10000))
                                    .handler(this::discover)
                    .failureHandler(this::timeout);
        }
        catch (Exception exception)
        {
            System.out.println("Error in discovery routing : "+exception.getMessage());
        }
    }

    private void timeout(RoutingContext context)
    {
        if (!context.response().ended())
        {
            context.response()
                    .setStatusCode(408)
                    .end("Request timed out.");
        }
    }

    //Creating discovery

    public void create(RoutingContext context)
    {
        try
        {
            var requestBody = context.body().asJsonObject();

            var discovery = new Discoveries(
                    null,
                    null,
                    requestBody.getString("discovery.name"),
                    requestBody.getString("discovery.ip"),
                    requestBody.getInteger("discovery.port"),
                    requestBody.getJsonArray("discovery.credential.profiles"),
                    "Down"
            );

            if (!Config.validIp(discovery.ip()))
            {
                context.response()
                        .setStatusCode(400)
                        .end(ApiResponse.error(400, "Invalid IP address provided", null).toJson());
                return;
            }

            if (Config.validPort(discovery.port()))
            {
                context.response()
                        .setStatusCode(400)
                        .end(ApiResponse.error(400, "Invalid port number provided", null).toJson());
                return;
            }

            // Insert into the database
            discoveryQuery.insert(discovery)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            Long discoveryID = result.result();
                            JsonObject response = new JsonObject()
                                    .put("discovery.id", discoveryID);
                            context.response()
                                    .setStatusCode(201)
                                    .end(ApiResponse.success(201, "Discovery created successfully", response).toJson());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(ApiResponse.error(500, "Failed to create discovery", result.cause().getMessage()).toJson());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in creating discovery",exception.getCause().getMessage()).toJson());
        }
    }

    //Fetching all discoveries from table

    public void getAll(RoutingContext context)
    {
        try
        {
            discoveryQuery.getAll()
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            JsonArray response = new JsonArray();

                            List<Discoveries> discoveries = result.result();

                            for(Discoveries discovery : discoveries)
                            {
                                response.add(new JsonObject()
                                        .put("discovery.id", discovery.discoveryId())
                                        .put("discovery.credential.profile", discovery.credential_profile())
                                        .put("discovery.name", discovery.name())
                                        .put("discovery.ip", discovery.ip())
                                        .put("discovery.port", discovery.port())
                                        .put("discovery.credential.profiles", discovery.credential_profiles())
                                        .put("discovery.status", discovery.status())
                                );
                            }
                            context.response()
                                    .setStatusCode(200)
                                    .end(ApiResponse.success(200, "Fetched discoveries successfully", response).toJson());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(404)
                                    .end(ApiResponse.error(404, "Error in fetching discoveries", result.cause().getMessage()).toJson());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in fetching discoveries",exception.getCause().getMessage()).toJson());
        }
    }

    //Fetching a discovery from table
    public void get(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            discoveryQuery.get(discoveryID)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            Discoveries discovery = result.result();

                            JsonObject response = new JsonObject()
                                    .put("discovery.id", discovery.discoveryId())
                                    .put("discovery.credential.profile", discovery.credential_profile())
                                    .put("discovery.name", discovery.name())
                                    .put("discovery.ip", discovery.ip())
                                    .put("discovery.port", discovery.port())
                                    .put("discovery.credential.profiles", discovery.credential_profiles())
                                    .put("discovery.status", discovery.status());

                            context.response()
                                    .setStatusCode(200)
                                    .end(ApiResponse.success(200, "Discovery fetched successfully", response).toJson());

                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Discovery not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(ApiResponse.error(404,"Discovery not found with this ID", result.cause().getMessage()).toJson());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(ApiResponse.error(500, "Error fetching discovery", result.cause().getMessage()).toJson());
                            }
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in fetching discovery of this ID",exception.getCause().getMessage()).toJson());
        }
    }

    //deleting discovery

    public void delete(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            discoveryQuery.delete(discoveryID)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            context.response()
                                    .setStatusCode(200)
                                    .end(ApiResponse.success(200, "Discovery deleted successfully", null).toJson());
                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Discovery not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(ApiResponse.error(404,"Not found", result.cause().getMessage()).toJson());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(ApiResponse.error(500, "Error deleting discovery", result.cause().getMessage()).toJson());
                            }
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in deleting discovery of this ID",exception.getCause().getMessage()).toJson());
        }
    }

    //Updating discovery

    public void update(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

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
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in updating discovery of this ID",exception.getCause().getMessage()).toJson());
        }

    }

    //Checking if device is up
    private void discover(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            discoveryQuery.fetch(discoveryID)
                    .compose(result->{
                        var ip = result.getString("ip_address");

                        var port = result.getInteger("port_number");

                        return vertx.<JsonObject>executeBlocking(pingFuture->{
                           try
                           {
                               if(Config.ping(ip))
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
                        try
                        {
                            if (Config.isPortOpen(deviceInfo.getString("ip"), deviceInfo.getInteger("port")))
                            {
//                                return Future.succeededFuture(deviceInfo.put("status","UP"));
                                deviceInfo.put("status","UP");
                            }
                            else
                            {
//                                return Future.failedFuture("Device is up but port is closed");
                                deviceInfo.put("status","DOWN");
                            }
                            return Future.succeededFuture(deviceInfo);
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("Error during port check: "+exception.getMessage());
                        }
                    })
                    .compose(deviceInfo->{
                        var status = deviceInfo.getString("status");

                        var timestamp = Timestamp.from(Instant.now());

                        return discoveryQuery.updateStatus(discoveryID,status,timestamp)
                                .compose(updateResult -> {
                                    if (updateResult)
                                    {
                                        return Future.succeededFuture("Device status updated in database");
                                    }
                                    else {
                                        return Future.failedFuture("Failed to update the database");
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
            context.response()
                    .setStatusCode(500)
                    .end("Error in checking status of this ID : "+exception.getMessage());
        }
    }
}
