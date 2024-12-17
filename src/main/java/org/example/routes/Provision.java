package org.example.routes;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.example.database.ProvisionQuery;
import org.example.database.QueryUtility;

public class Provision
{
    private final ProvisionQuery provisionQuery = new ProvisionQuery();

    private final QueryUtility queryHandler = new QueryUtility();

    private static final Logger logger = LoggerFactory.getLogger(Provision.class);

    public void route(Router provisionRouter)
    {
        try
        {
            provisionRouter.post("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::provision)
                    .failureHandler(this::timeout);
        }
        catch(Exception exception)
        {
            logger.error("Error in credential routing", exception);
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

            provisionQuery.insert(id)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            Long objectID = result.result();

                            context.response()
                                    .setStatusCode(201)
                                    .end(new JsonObject()
                                            .put("status.code",201)
                                            .put("message","Device was successfully provisioned")
                                            .put("data",new JsonObject()
                                                    .put("object.id", objectID)).encodePrettily());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject()
                                            .put("status.code",500)
                                            .put("message","Failed to provision device")
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
                            .put("message","Server error in checking if the device is up or not")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

}
