package org.example.poll;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.example.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.example.database.QueryUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scheduler extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private static final int PERIODIC_INTERVAL = 5000;

    private final Map<Long,JsonObject> pollDevices = new HashMap<>(); //Will contain provisioned devices

    public void start()
    {
        getDevices()
                .onSuccess(v->
                {
                    logger.info("Provisioned devices in database fetched successfully.");

                    vertx.setPeriodic(PERIODIC_INTERVAL,id-> checkAndPreparePolling());
                })
                .onFailure(error->
                {
                    logger.info("Failed to fetch provisioned devices : {}",error.getMessage());
                });
    }

    //To fetch provisioned devices present in database
    private Future<Void> getDevices()
    {
        Promise<Void> promise = Promise.promise();

        QueryUtility.getInstance().getAll("objects")
                .onSuccess(objectsArray->
                {
                    if (objectsArray.isEmpty())
                    {
                        promise.fail("There are no provisioned devices currently");
                    }
                    else
                    {
                        logger.info("Fetched {} rows from 'objects' table", objectsArray.size());

                        for (int i = 0; i < objectsArray.size(); i++)
                        {
                            // Object will contain information like object_id, credential_profile, ip, and hostname
                            var object = objectsArray.getJsonObject(i);

                            var objectId = object.getLong("object_id");

                            fetchMetricData(objectId)
                                    .onSuccess(metrics ->
                                    {
                                        var deviceMetrics = new JsonObject()
                                                .put("device", object)
                                                .put("metrics", metrics);

                                        pollDevices.put(objectId, deviceMetrics);
                                    })
                                    .onFailure(err -> logger.error("Failed to fetch metrics {}: {}", objectId, err.getMessage()));
                        }
                        promise.complete();
                    }
                })
                .onFailure(error->
                {
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    private Future<JsonArray> fetchMetricData(Long objectId)
    {
        Promise<JsonArray> promise = Promise.promise();
        // Fetch all rows from the 'metric_object' table for the given object_id

        var metricColumns = List.of("metric_group_name", "metric_poll_time", "last_polled");

        QueryUtility.getInstance().get("metrics",metricColumns,new JsonObject().put("metric_object",objectId))
                .onSuccess(metric->
                {
                    var metricArray = metric.getJsonArray("data");

                    promise.complete(metricArray);
                })
                .onFailure(error ->
                {
                    logger.error("Failed to fetch metrics for object {}: {}", objectId, error.getMessage());

                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    private void checkAndPreparePolling()
    {
        for (Map.Entry<Long, JsonObject> entry : pollDevices.entrySet())
        {
            var deviceMetrics = entry.getValue();

            var device = deviceMetrics.getJsonObject("device");

            var metrics = deviceMetrics.getJsonArray("metrics");

            for (int i = 0; i < metrics.size(); i++)
            {
                var metricData = metrics.getJsonObject(i);

                preparePolling(device, metricData);
            }
        }
    }

    private void preparePolling(JsonObject objectData, JsonObject metricData)
    {
        var pollTime = metricData.getInteger("metric_poll_time") * 1000L; //To convert into millisecond

        var lastPolled = metricData.getLong("last_polled");

        var currentTime = System.currentTimeMillis();

        if(lastPolled == null || currentTime - lastPolled >= pollTime)
        {
            vertx.eventBus().send(Constants.OBJECT_POLL,new JsonObject()
                                                        .put("object.id",objectData.getLong("object_id"))
                                                        .put("credential.profile",objectData.getLong("credential_profile"))
                                                        .put("ip",objectData.getString("ip"))
                                                        .put("metric.group.name",metricData.getString("metric_group_name")));

            logger.info("Polling triggered for {} at {}", objectData.getString("hostname"), objectData.getString("ip"));
        }
    }
}
