package org.example.routes;

import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.example.database.QueryUtility;
import org.example.utilities.Util;

import java.util.ArrayList;
import java.util.List;

import static org.example.Bootstrap.vertx;

public class Discovery implements CrudOperations
{
    private static final Logger logger = LoggerFactory.getLogger(Discovery.class);

    public void route(Router discoveryRouter)
    {
        try
        {
            discoveryRouter.post("/create").handler(this::create);

            discoveryRouter.put("/:id").handler(this::update);

            discoveryRouter.get("/").handler(this::getAll);

            discoveryRouter.get("/:id").handler(this::get);

            discoveryRouter.delete("/:id").handler(this::delete);

            discoveryRouter.post("/:id/run").handler(this::discover);
        }
        catch (Exception exception)
        {
            logger.error("Error in discovery routing", exception);
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

            if (!Util.validIp(ip))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid IP address provided").encodePrettily());
                return;
            }

            if (Util.validPort(port))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid port provided").encodePrettily());
                return;
            }

            QueryUtility.getInstance().get("discoveries", List.of("discovery_id"), new JsonObject().put("name", name))
                    .compose(result ->
                    {
                        if (!result.containsKey("error"))
                        {
                            // If discovery name already exists, return a failed future
                            return Future.failedFuture("Discovery name should be unique");
                        }
                        else
                        {
                            // If name is unique, insert
                            return QueryUtility.getInstance().insert("discoveries",new JsonObject()
                                    .put("name",name)
                                    .put("ip",ip)
                                    .put("port",port)
                                    .put("credential_profiles",credential_profiles)
                                    .put("status","Down"));
                        }
                    })
                    .onComplete(result->
                    {
                        if(result.succeeded())
                        {
                            context.response().setStatusCode(201).end(new JsonObject()
                                            .put("status.code",201).put("message","Discovery created successfully")
                                            .put("data",new JsonObject().put("discovery.id", result.result())).encodePrettily());
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
            if (!Util.validIp(ip))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid IP address provided").encodePrettily());
                return;
            }

            if (Util.validPort(port))
            {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("status.code",400)
                                .put("message","Invalid port provided").encodePrettily());
                return;
            }

            var id = Long.parseLong(discoveryID);

            QueryUtility.getInstance().update("discoveries",new JsonObject()
                                    .put("name",name)
                                    .put("ip",ip)
                                    .put("port",port)
                                    .put("credential_profiles",credential_profiles),new JsonObject().put("discovery_id",id))

                    .onComplete(result->
                    {
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
                            if (result.cause().getMessage().contains("No matching rows found"))
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
            var id = Long.parseLong(discoveryID);

            QueryUtility.getInstance().delete("discoveries","discovery_id",id)
                    .onComplete(result->
                    {
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
            var id = Long.parseLong(discoveryID);

            var columns = List.of("credential_profile", "name", "ip","port","credential_profiles","status","hostname");

            QueryUtility.getInstance().get("discoveries",columns,new JsonObject().put("discovery_id",id))
                    .onComplete(result->
                    {
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
                            if (result.result().containsKey("error"))
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
            QueryUtility.getInstance().getAll("discoveries")
                    .onComplete(result->
                    {
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
            var id = Long.parseLong(discoveryID);

            var columns = List.of("ip","port","credential_profiles");

                //I will check whether this discovery ID is present in database or not
                QueryUtility.getInstance().get("discoveries",columns,new JsonObject().put("discovery_id",id))
                        .compose(deviceInfo->
                        {
                            if (deviceInfo.containsKey("error"))
                            {
                                return Future.failedFuture("This discovery ID was not found in the database");
                            }

                            List<String> values = List.of("object_id");

                            // Then I will check whether device is provisioned already or not
                            return QueryUtility.getInstance().get("objects", values, new JsonObject().put("ip", deviceInfo.getString("ip")))
                                    .compose(objectInfo ->
                                    {
                                        if (!objectInfo.containsKey("error"))
                                        {
                                            return Future.failedFuture("Device is already provisioned");
                                        }

                                        return Future.succeededFuture(deviceInfo);
                                    });
                        })
                    .compose(deviceInfo->
                    {

                        var profiles = deviceInfo.getJsonArray("credential_profiles");

                        List<Future<JsonObject>>credentialFutures = new ArrayList<>();

                        if(profiles.isEmpty())
                        {
                            return Future.failedFuture("No credential profiles found for this ID");
                        }
                        for (int i = 0; i < profiles.size(); i++)
                        {
                            Long profileID = profiles.getLong(i);

                            List<String> fields = List.of("profile_id,profile_protocol","user_name","user_password","community","version");

                            Future<JsonObject> credentialFuture = QueryUtility.getInstance().get("credentials", fields, new JsonObject().put("profile_id",profileID))

                                    .onSuccess(result -> {
                                        logger.info("Credential fetch succeeded for profile ID {}", profileID);
                                    })
                                    .onFailure(err -> {
                                        logger.error("Credential for profile ID {} not found: {}", profileID, err.getMessage());
                                    });

                            credentialFutures.add(credentialFuture);
                        }

                        return Future.join(credentialFutures)
                                .map(compositeFuture ->
                                {
                                    var profileData = new JsonArray();

                                    for (int i = 0; i < compositeFuture.size(); i++)
                                    {
                                        JsonObject result = compositeFuture.resultAt(i);

                                        if (!result.containsKey("error"))
                                        {
                                            profileData.add(result);
                                        }
                                    }
                                    deviceInfo.put("discovery.credential.profiles", profileData);

                                    return deviceInfo;
                                });
                    })
                    .compose(deviceInfo -> vertx.<JsonObject>executeBlocking(pingFuture ->
                    {
                        try
                        {
                            if (Util.ping(deviceInfo.getString("ip")))
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
                    .compose(deviceInfo ->
                    {
                        try
                        {
                            QueryUtility.getInstance().get("discoveries", List.of("port"), new JsonObject().put("discovery_id", id))
                                    .compose(portResult ->
                                    {
                                        Integer port = portResult.getInteger("port");

                                        if (port == 161)
                                        {
                                            return Future.succeededFuture(deviceInfo);
                                        }

                                        // Check if the port is open
                                        return Future.future(promise ->
                                        {
                                            if (Util.isPortOpen(deviceInfo.getString("ip"), port))
                                            {
                                                promise.complete(deviceInfo);
                                            }
                                            else
                                            {
                                                promise.fail("Ping done but port is closed for the specified connection");
                                            }

                                        });
                                    });
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("There was problem in fetching port");
                        }

                        return Future.succeededFuture(deviceInfo);

                    })
                    .compose(deviceInfo ->
                    {
                        try
                        {
                            return validCredential(deviceInfo);
                        }
                        catch (Exception exception)
                        {
                            return Future.failedFuture("Error during finding valid credential profile " + exception.getMessage());
                        }
                    })
                    .compose(deviceInfo ->
                    {
                        try
                        {
                            // Update the status in the database after removing credential profiles
                            deviceInfo.remove("discovery.credential.profiles");

                            return QueryUtility.getInstance().update("discoveries",deviceInfo,new JsonObject().put("discovery_id",id))
                                    .compose(updateResult -> {
                                        if (updateResult)
                                        {
                                            return Future.succeededFuture("Device status updated in database");
                                        }
                                        else
                                        {
                                            return Future.failedFuture("Failed to update the database for the discoveryID");
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
        return vertx.executeBlocking(promise ->
        {
            try
            {
                if (Util.checkConnection(deviceInfo))
                {
                    deviceInfo.put("status", "Up");
                }
                else
                {
                    deviceInfo.put("credential_profile", null);

                    deviceInfo.put("status", "Down");

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
