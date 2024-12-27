package org.example.routes;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.example.database.QueryUtility;
import org.example.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.example.Bootstrap.vertx;

public class Provision
{
    private static final Logger logger = LoggerFactory.getLogger(Provision.class);

    private final HashMap<String, Integer> linuxMetrics = new HashMap<>()
    {{
        put("Linux.Device", Constants.DEVICE_POLL_INTERVAL);

        put("Linux.CPU", Constants.CPU_POLL_INTERVAL);

        put("Linux.Process", Constants.PROCESS_POLL_INTERVAL);

        put("Linux.Disk", Constants.DISK_POLL_INTERVAL);
    }};

    private final HashMap<String, Integer> snmpMetrics = new HashMap<>()
    {{
        put("SNMP.Device", Constants.SNMP_POLL_INTERVAL);

        put("SNMP.Interface", Constants.INTERFACE_POLL_INTERVAL);
    }};

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
            var id = Long.parseLong(discoveryID);

            var columns = List.of("credential_profile","ip","port","device_type","status","hostname");

            //First I will check do discoveryID exists or not
            QueryUtility.getInstance().get(Constants.DISCOVERIES, columns, new JsonObject().put("discovery_id", id))
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
                        // Secondly I will check if device is already provisioned or not
                        return QueryUtility.getInstance()
                                .get(Constants.OBJECTS, List.of("ip"), new JsonObject().put("ip", discoveryInfo.getString("ip")))

                                .compose(existingObject ->
                                {
                                    if (!existingObject.containsKey("error"))
                                    {
                                        return Future.failedFuture("Device is already provisioned with this IP Address");
                                    }
                                    return Future.succeededFuture(discoveryInfo);
                                });
                    })
                    .compose(discoveryInfo ->
                    {
                        // Proceed to insert the device into the 'objects' table
                        return QueryUtility.getInstance().insert(Constants.OBJECTS, new JsonObject()
                                .put("credential_profile", discoveryInfo.getLong("credential_profile"))
                                .put("ip", discoveryInfo.getString("ip"))
                                .put("hostname", discoveryInfo.getString("hostname"))
                                        .put("device_type",discoveryInfo.getString("device_type")))
                                .map(insertedId ->
                                {
                                    discoveryInfo.put("object_id", insertedId);

                                    return discoveryInfo; // Return discoveryInfo for next steps
                                });
                    })
                    .compose(discoveryInfo ->
                    {
                        // Attaching metrics for the provisioned object
                        List<Future<Long>> metricFutures = new ArrayList<>();

                        (Objects.equals(discoveryInfo.getString("device_type"), "Linux") ? linuxMetrics : snmpMetrics).forEach((key, value) ->
                        {
                            metricFutures.add(QueryUtility.getInstance().insert(Constants.METRICS, new JsonObject()
                                    .put("metric_group_name", key)
                                    .put("metric_poll_time", value)
                                    .put("metric_object", discoveryInfo.getLong("object_id"))));
                        });

                        //If attaching any metric fails , no need to proceed further
                        return Future.all(metricFutures).map(discoveryInfo);
                    })
                    .onSuccess(result ->
                    {
                        vertx.eventBus().send(Constants.OBJECT_PROVISION,new JsonObject()
                                .put("object_id",result.getLong("object_id"))
                                .put("credential_profile", result.getLong("credential_profile"))
                                .put("ip",result.getString("ip"))
                                .put("hostname",result.getString("hostname"))
                                .put("device_type",result.getString("device_type")));

                        context.response()
                                .setStatusCode(201)
                                .end(new JsonObject()
                                        .put("status.code",201)
                                        .put("message","Device provisioned successfully")
                                        .put("data",new JsonObject()
                                                .put("object.id", result.getLong("object_id"))).encodePrettily());
                    })
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
