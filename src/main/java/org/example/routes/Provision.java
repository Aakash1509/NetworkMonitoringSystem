package org.example.routes;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.example.database.QueryUtility;

import java.util.List;

public class Provision
{
    private final QueryUtility queryHandler = QueryUtility.getInstance();

    private static final Logger logger = LoggerFactory.getLogger(Provision.class);

    public void route(Router provisionRouter)
    {
        try
        {
            provisionRouter.post("/:id").handler(this::provision);

        }
        catch(Exception exception)
        {
            logger.error("Error in provision routing", exception);
        }
    }

    private void provision(RoutingContext context)
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

            List<String> columns = List.of("credential_profile","ip","status","hostname");

            queryHandler.get("discoveries",columns,new JsonObject().put("discovery_id",id))
                    .compose(discoveryInfo ->
                    {
                        if (discoveryInfo.containsKey("error"))
                        {
                            return Future.failedFuture("This discovery ID was not found in the database");
                        }
                        if (!"Up".equals(discoveryInfo.getString("status")))
                        {
                            return Future.failedFuture("Device is down so cannot be provisioned");
                        }
                        return queryHandler.insert("objects", new JsonObject()
                                .put("credential_profile",discoveryInfo.getLong("credential_profile"))
                                .put("ip",discoveryInfo.getString("ip"))
                                .put("hostname",discoveryInfo.getString("hostname")));

                    })
                    .onSuccess(result -> context.response()
                            .setStatusCode(201)
                            .end(new JsonObject()
                                    .put("status.code",201)
                                    .put("message","Device provisioned successfully")
                                    .put("data",new JsonObject()
                                            .put("object.id", result)).encodePrettily()))

                    .onFailure(error -> context.response()
                            .setStatusCode(400)
                            .end(new JsonObject()
                                    .put("status.code",400)
                                    .put("message","Device cannot be provisioned")
                                    .put("error",error.getMessage()).encodePrettily()));
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

}
