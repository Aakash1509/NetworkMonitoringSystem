package org.example.routes;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.example.database.DiscoveryQuery;
import org.example.utils.Config;


import static org.example.Bootstrap.vertx;

public class Discovery implements CrudOperations
{
    private final DiscoveryQuery discoveryQuery = new DiscoveryQuery();

    private static final Logger logger = LoggerFactory.getLogger(Discovery.class);

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

            discoveryRouter.get("/:id/fetch")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::fetchDiscovery)
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
            logger.error("Error in discovery routing", exception);
        }
    }

    private void timeout(RoutingContext context)
    {
        if (!context.response().ended())
        {
            context.response()
                    .setStatusCode(408)
                    .end(new JsonObject()
                            .put("status.code",408)
                            .put("message","Request Timed Out")
                            .put("error","The server timed out waiting for the request").encodePrettily());
        }
    }

    //Creating discovery
    @Override
    public void create(RoutingContext context)
    {
        try
        {
            var requestBody = context.body().asJsonObject();

            var name = requestBody.getString("discovery.name");

            var ip = requestBody.getString("discovery.ip");

            var port = requestBody.getInteger("discovery.port");

            var credential_profiles = requestBody.getJsonArray("discovery.credential.profiles");

            if (!Config.validIp(ip))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid IP address provided").encodePrettily());
                return;
            }

            if (Config.validPort(port))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid port provided").encodePrettily());
                return;
            }

            // Insert into the database
            discoveryQuery.insert(name,ip,port,credential_profiles,"DOWN")
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            Long discoveryID = result.result();
                            JsonObject response = new JsonObject()
                                    .put("discovery.id", discoveryID);
                            
                            context.response()
                                    .setStatusCode(201)
                                    .end(new JsonObject()
                                            .put("status.code",201)
                                            .put("message","Discovery created successfully")
                                            .put("data",response).encodePrettily());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject()
                                            .put("status.code",500)
                                            .put("message","Failed to create discovery")
                                            .put("error",result.cause().getMessage()).encodePrettily());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in creating discovery")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Updating discovery
    @Override
    public void update(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            var requestBody = context.body().asJsonObject();

            var name = requestBody.getString("discovery.name");

            var ip = requestBody.getString("discovery.ip");

            var port = requestBody.getInteger("discovery.port");

            var credential_profiles = requestBody.getJsonArray("discovery.credential.profiles");

            if (!Config.validIp(ip))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid IP address provided").encodePrettily());
                return;
            }

            if (Config.validPort(port))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid port provided").encodePrettily());
                return;
            }

            discoveryQuery.update(discoveryID,name,ip,port,credential_profiles)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            context.response()
                                    .setStatusCode(200)
                                    .end(new JsonObject()
                                            .put("status.code",200)
                                            .put("message","Discovery updated successfully").encodePrettily());
                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Discovery not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code",404)
                                                .put("message","Discovery not found of this ID")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(new JsonObject()
                                                .put("status.code",500)
                                                .put("message","Database error while updating discovery")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                        }
                    });

        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in updating discovery")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }

    }

    //Deleting discovery
    @Override
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
                                    .end(new JsonObject()
                                            .put("status.code",200)
                                            .put("message","Discovery deleted successfully").encodePrettily());
                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Discovery not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code",404)
                                                .put("message","Discovery not found of this ID")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(new JsonObject()
                                                .put("status.code",500)
                                                .put("message","Database error while deleting discovery")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in deleting discovery")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Fetching a discovery
    @Override
    public void get(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            discoveryQuery.get(discoveryID)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            context.response()
                                    .setStatusCode(200)
                                    .end(new JsonObject()
                                            .put("status.code",200)
                                            .put("message","Discovery fetched successfully")
                                            .put("data",result.result()).encodePrettily());
                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Discovery not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code",404)
                                                .put("message","Discovery not found of this ID")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(new JsonObject()
                                                .put("status.code",500)
                                                .put("message","Database error while fetching discovery")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in fetching discovery for given ID")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Fetching all discoveries
    @Override
    public void getAll(RoutingContext context)
    {
        try
        {
            discoveryQuery.getAll()
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            var discoveries = result.result();

                            if(!discoveries.isEmpty())
                            {
                                context.response()
                                        .setStatusCode(200)
                                        .end(new JsonObject()
                                                .put("status.code",200)
                                                .put("message","Discoveries fetched successfully")
                                                .put("data",discoveries).encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(200)
                                        .end(new JsonObject()
                                                .put("status.code",200)
                                                .put("message","Discoveries fetched successfully")
                                                .put("data","No discovery currently").encodePrettily());
                            }
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject()
                                            .put("status.code",500)
                                            .put("message","Database error while fetching discoveries")
                                            .put("error",result.cause().getMessage()).encodePrettily());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in fetching discoveries")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    public void fetchDiscovery(RoutingContext context) {
        try {
            // Extract the discovery ID from the path parameter
            Long discoveryID = Long.parseLong(context.pathParam("id"));

            // Execute the query to fetch discovery details using JOIN
            discoveryQuery.fetch(discoveryID)
                    .onComplete(result -> {
                        if (result.succeeded()) {
                            // Process the result
                            JsonObject discovery = result.result();

                            // Create response JSON object
                            JsonObject response = new JsonObject()
                                    .put("discovery.ip", discovery.getString("discovery.ip"))
                                    .put("discovery.port", discovery.getString("discovery.port"));

                            // Extract credentials if present
                            JsonArray credentialsArray = discovery.getJsonArray("discovery.credential.profiles");
                            if (credentialsArray != null) {
                                response.put("discovery.credential.profiles", credentialsArray);
                            } else {
                                response.put("discovery.credential.profiles", new JsonArray());
                            }

                            // Send success response
                            context.response()
                                    .setStatusCode(200)
                                    .end(new JsonObject()
                                            .put("status.code", 200)
                                            .put("message", "Discovery fetched successfully")
                                            .put("data", response).encodePrettily());
                        } else {
                            // Handle failure cases
                            if (result.cause().getMessage().contains("No record found")) {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code", 404)
                                                .put("message", "Discovery not found for this ID")
                                                .put("error", result.cause().getMessage()).encodePrettily());
                            } else {
                                context.response()
                                        .setStatusCode(500)
                                        .end(new JsonObject()
                                                .put("status.code", 500)
                                                .put("message", "Database error while fetching discovery")
                                                .put("error", result.cause().getMessage()).encodePrettily());
                            }
                        }
                    });
        } catch (Exception exception) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code", 500)
                            .put("message", "Server error in fetching discovery for given ID")
                            .put("error", exception.getMessage()).encodePrettily());
        }
    }

    //Checking if device is up
    private void discover(RoutingContext context)
    {
        try
        {
            var discoveryID = Long.parseLong(context.pathParam("id"));

            // Function will be here to check whether device is provisioned already or not
            discoveryQuery.fetch(discoveryID)
                    .compose(deviceInfo -> vertx.<JsonObject>executeBlocking(pingFuture -> {
                        try
                        {
                            if (Config.ping(deviceInfo.getString("discovery.ip")))
                            {
                                pingFuture.complete(deviceInfo);
                            }
                            else
                            {
                                pingFuture.fail("Device is down, ping failed");
                            }
                        }
                        catch (Exception exception)
                        {
                            pingFuture.fail("Error during ping: " + exception.getMessage());
                        }
                    }))
                    .compose(deviceInfo -> {
                        try
                        {
                            if (Config.isPortOpen(deviceInfo.getString("discovery.ip"), deviceInfo.getInteger("discovery.port")))
                            {
                                return Future.succeededFuture(deviceInfo);
                            }
                            else
                            {
                                return Future.failedFuture("Ping done but port is closed");
                            }
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("Error during port check: " + exception.getMessage());
                        }
                    })
                    .compose(deviceInfo -> {
                        try
                        {
                            return validCredential(deviceInfo);
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("Error during finding valid credential profile " + exception.getMessage());
                        }
                    })
                    .compose(deviceInfo -> {
                        try
                        {
                            // Update the status in the database
                            return discoveryQuery.updateStatus(deviceInfo.getLong("credential.profile"), deviceInfo.getString("status"), deviceInfo.getString("hostname"),discoveryID)
                                    .compose(updateResult -> {
                                        if (updateResult)
                                        {
                                            return Future.succeededFuture("Device status updated in database");
                                        }
                                        else
                                        {
                                            return Future.failedFuture("Failed to update the database for discoveryID: " + discoveryID);
                                        }
                                    });
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("Error during updating status of device in database " + exception.getMessage());
                        }
                    })
                    .onSuccess(finalResult -> context.response().setStatusCode(200).end(new JsonObject().put("status.code",200).put("message",finalResult).encodePrettily()))

                    .onFailure(error -> context.response().setStatusCode(400).end(new JsonObject().put("status.code",400).put("message",error.getMessage()).encodePrettily()));
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in checking if the device is up or not")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    private Future<JsonObject> validCredential(JsonObject deviceInfo)
    {
        return vertx.executeBlocking(promise -> {
            try
            {
                if (Config.checkConnection(deviceInfo))
                {
                    deviceInfo.put("status", "Up");
                }
                else
                {
                    deviceInfo.put("status", "Down");
                    deviceInfo.put("credential.profile", null);
                    deviceInfo.put("hostname", null);
                }
                promise.complete(deviceInfo);
            }
            catch (Exception exception)
            {
                promise.fail("Error during finding valid credential profile: " + exception.getMessage());
            }
        });
    }

}




