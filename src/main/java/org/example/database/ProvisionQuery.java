package org.example.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import static org.example.Bootstrap.client;

public class ProvisionQuery
{
    public Future<Long> insert(long discoveryID)
    {
        Promise<Long> promise = Promise.promise();

        client.preparedQuery("INSERT INTO objects (credential_profile, ip, hostname) " +
                        "SELECT credential_profile, ip, hostname " +
                        "FROM discoveries " +
                        "WHERE discovery_id = $1 AND status = 'Up' " +
                        "RETURNING object_id"
                )
                .execute(Tuple.of(discoveryID), execute -> {
                    if (execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();
                        if (rows.size() > 0)
                        {
                            Long objectId = rows.iterator().next().getLong("object_id");
                            promise.complete(objectId);
                        }
                        else {
                            promise.fail("Device is not UP or not found Discovery ID: " + discoveryID);
                        }
                    }
                    else
                    {
                        var error = execute.cause().getMessage();

                        if(error.contains("unique_ip_hostname"))
                        {
                            promise.fail("Device with this IP and hostname is already provisioned.");
                        }
                        else
                        {
                            promise.fail(error);
                        }
                    }
                });
        return promise.future();
    }
}
