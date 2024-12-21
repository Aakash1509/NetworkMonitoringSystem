package org.example.poll;

import io.vertx.core.AbstractVerticle;
import org.example.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.example.database.QueryUtility;

import java.util.List;

public class Scheduler extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private static final int PERIODIC_INTERVAL = 5000;

    public void start()
    {

        vertx.setPeriodic(PERIODIC_INTERVAL,id-> checkAndPreparePolling());
    }

    private void checkAndPreparePolling()
    {
        // Fetch all provisioned devices from the 'objects' table

        QueryUtility.getInstance().getAll("objects")
                .onSuccess(objectsArray ->
                {
                    // Successfully fetched all objects
                    logger.info("Fetched {} rows from 'objects' table", objectsArray.size());

                    // Iterate over the JSON array of objects
                    for (int i = 0; i < objectsArray.size(); i++)
                    {
                        //Object will contain information like object_id,credential_profile,ip and hostname
                        JsonObject object = objectsArray.getJsonObject(i);

                        Long objectId = object.getLong("object_id");

                        // Fetch metric data for each object
                        fetchMetricData(objectId, object);
                    }
                })
                .onFailure(error ->
                {
                    // Handle failure when fetching from the 'objects' table
                    logger.error("Failed to fetch data from 'objects' table: {}", error.getMessage());
                });
    }

    private void fetchMetricData(Long objectId, JsonObject objectData)
    {
        // Fetch all rows from the 'metric_object' table for the given object_id
        List<String> metricColumns = List.of("metric_group_name", "metric_poll_time", "last_polled");

        QueryUtility.getInstance().get("metrics",metricColumns,new JsonObject().put("metric_object",objectId))
                .onSuccess(metric ->
                {
                    var metricArray = metric.getJsonArray("data");

                    // Perform polling logic for each metric associated with the object
                    for (int i = 0; i < metricArray.size(); i++)
                    {
                        //Metric data will consist of the metrics associated with one device like group name , poll time and when it was polled last time
                        JsonObject metricData = metricArray.getJsonObject(i);

                        preparePolling(objectData, metricData);
                    }
                })
                .onFailure(error ->
                {
                    // Handle failure when fetching metric data
                    logger.error("Failed to fetch metric data for object {}: {}", objectId, error.getMessage());
                });
    }

    private void preparePolling(JsonObject objectData, JsonObject metricData)
    {
        var pollTime = metricData.getInteger("metric_poll_time") * 1000L; //To convert into millisecond

        var lastPolled = metricData.getLong("last_polled");

        var currentTime = System.currentTimeMillis();

        if(lastPolled == null || currentTime - lastPolled >= pollTime)
        {
            vertx.eventBus().send(Constants.OBJECT_POLL,new JsonObject()
                                                        .put("object_id",objectData.getLong("object_id"))
                                                        .put("credential_profile",objectData.getLong("credential_profile"))
                                                        .put("ip",objectData.getString("ip"))
                                                        .put("metric_group_name",metricData.getString("metric_group_name")));

            logger.info("Polling triggered for {} at {}", objectData.getString("hostname"), objectData.getString("ip"));

        }
    }
}
