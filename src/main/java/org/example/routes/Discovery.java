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
import org.example.database.QueryUtility;
import org.example.utils.Config;


import java.util.Objects;

import static org.example.Bootstrap.client;
import static org.example.Bootstrap.vertx;

public class Discovery implements CrudOperations
{
    private final DiscoveryQuery discoveryQuery = new DiscoveryQuery();

    private final QueryUtility queryHandler = new QueryUtility();

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

            if(name == null || name.isEmpty() || ip == null || ip.isEmpty() || port == null || credential_profiles == null || credential_profiles.isEmpty())
            {
                context.response()
                        .setStatusCode(500)
                        .end(new JsonObject()
                                .put("status.code",500)
                                .put("message","Please enter required fields").encodePrettily());

                return;
            }

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
            queryHandler.insert("discoveries",new JsonObject()
                            .put("name",name)
                            .put("ip",ip)
                            .put("port",port)
                            .put("credential_profiles",credential_profiles)
                            .put("status","Down"))
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            Long discoveryID = result.result();

                            context.response()
                                    .setStatusCode(201)
                                    .end(new JsonObject()
                                            .put("status.code",201)
                                            .put("message","Discovery created successfully")
                                            .put("data",new JsonObject()
                                                    .put("discovery.id", discoveryID)).encodePrettily());
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
        var discoveryID = context.pathParam("id");

        var requestBody = context.body().asJsonObject();

        var name = requestBody.getString("discovery.name");

        var ip = requestBody.getString("discovery.ip");

        var port = requestBody.getInteger("discovery.port");

        var credential_profiles = requestBody.getJsonArray("discovery.credential.profiles");

        if(name == null || name.isEmpty() || ip == null || ip.isEmpty() || port == null || credential_profiles == null || credential_profiles.isEmpty())
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Please enter required fields").encodePrettily());

            return;
        }
        try
        {
            long id = Long.parseLong(discoveryID);

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

            queryHandler.update("discoveries",new JsonObject()
                            .put("name",name)
                            .put("ip",ip)
                            .put("port",port)
                            .put("credential_profiles",credential_profiles)
                            .put("discovery_id",id))
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
                            if (result.cause().getMessage().contains("Information not found"))
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

        var discoveryID = context.pathParam("id");

        if (discoveryID == null || discoveryID.isEmpty())
        {
            context.response()
                    .setStatusCode(404)
                    .end(new JsonObject()
                            .put("status.code", 404)
                            .put("message", "Please enter a valid discovery ID").encodePrettily());
            return;
        }
        try
        {
            long id = Long.parseLong(discoveryID);

            queryHandler.delete("discoveries","discovery_id",id)
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
                            if (result.cause().getMessage().contains("Information not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code",404)
                                                .put("message","Discovery not found of this ID").encodePrettily());
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
        var discoveryID = context.pathParam("id");

        if (discoveryID == null || discoveryID.isEmpty())
        {
            context.response()
                    .setStatusCode(404)
                    .end(new JsonObject()
                            .put("status.code", 404)
                            .put("message", "Please enter a valid discovery ID").encodePrettily());
            return;
        }
        try
        {
            long id = Long.parseLong(discoveryID);

            queryHandler.get("discoveries","discovery_id",id)
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
                            if (result.cause().getMessage().contains("Information not found"))
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
            queryHandler.getAll("discoveries")
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

    //Checking if device is up
    private void discover(RoutingContext context)
    {
        var discoveryID = context.pathParam("id");

        if (discoveryID == null || discoveryID.isEmpty())
        {
            context.response()
                    .setStatusCode(404)
                    .end(new JsonObject()
                            .put("status.code", 404)
                            .put("message", "Please enter a valid discovery ID").encodePrettily());
            return;
        }
        try
        {
            long id = Long.parseLong(discoveryID);

            // Function will be here to check whether device is provisioned already or not
            queryHandler.get("discoveries","discovery_id",id)
                    .compose(deviceInfo -> vertx.<JsonObject>executeBlocking(pingFuture -> {
                        try
                        {
                            if (Config.ping(deviceInfo.getString("ip")))
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
                            if(Objects.equals(deviceInfo.getString("discovery.protocol"), "SSH"))
                            {
                                if (Config.isPortOpen(deviceInfo.getString("discovery.ip"), deviceInfo.getInteger("discovery.port")))
                                {
                                    return Future.succeededFuture(deviceInfo);
                                }
                                else
                                {
                                    return Future.failedFuture("Ping done but port is closed for an SSH connection");
                                }
                            }
                            else
                            {
                                return Future.succeededFuture(deviceInfo);
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
                            return discoveryQuery.updateStatus(deviceInfo.getLong("credential.profile"), deviceInfo.getString("status"), deviceInfo.getString("hostname"),id)
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
