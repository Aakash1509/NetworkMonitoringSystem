package org.example.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.example.Bootstrap;
import org.example.store.Config;

import java.util.ArrayList;
import java.util.List;

public class QueryUtility
{
    private static QueryUtility instance;

    private QueryUtility()
    {

    }

    public static QueryUtility getInstance()
    {
        if(instance==null)
        {
            instance = new QueryUtility();
        }
        return instance;
    }

    private static PgPool client;

    static
    {
        // Database connection options
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(Config.DB_PORT)
                .setHost(Config.DB_HOST)
                .setDatabase(Config.DB_DATABASE)
                .setUser(Config.DB_USER)
                .setPassword(Config.DB_PASSWORD);

        // Pool options
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(Config.POOL_SIZE); // Maximum connections in the pool

        // Create client
        client = PgPool.pool(Bootstrap.vertx, connectOptions, poolOptions);
    }

    public Future<Long> insert(String tableName, JsonObject data)
    {
        Promise<Long> promise = Promise.promise();

        var columns = new StringBuilder();

        var placeholders = new StringBuilder();

        List<Object> values = new ArrayList<>();

        data.forEach(entry ->
        {
            columns.append(entry.getKey()).append(", ");

            placeholders.append("$").append(values.size() + 1).append(", ");

            values.add(entry.getValue());
        });

        columns.setLength(columns.length() - 2);

        placeholders.setLength(placeholders.length() - 2);

        client.preparedQuery("INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ") RETURNING *") //* means every column
                .execute(Tuple.from(values), execute -> {
                    if (execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        if (rows.size() > 0)
                        {
                            var id = rows.iterator().next().getLong(0);

                            promise.complete(id);
                        }
                        else
                        {
                            promise.fail("Some problem");
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause().getMessage());
                    }
                });
        return promise.future();
    }

    public Future<Void> delete(String tableName, String column, Long id)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("DELETE FROM "+ tableName + " WHERE " + column + " = $1")
                .execute(Tuple.of(id), execute ->{
                    if(execute.succeeded())
                    {
                        if(execute.result().rowCount()>0)
                        {
                            promise.complete();
                        }
                        else
                        {
                            promise.fail("Information not found");
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });

        return promise.future();
    }

    public Future<JsonArray> getAll(String tableName)
    {
        Promise<JsonArray> promise = Promise.promise();


        client.preparedQuery("SELECT * FROM "+ tableName)
                .execute(execute->{
                    if(execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        var response = new JsonArray();

                        for(Row row : rows)
                        {
                            var json = new JsonObject();

                            for(int i=0;i<row.size();i++)
                            {
                                var columnName = row.getColumnName(i);

                                var value = row.getValue(i);

                                json.put(columnName, value);
                            }
                            response.add(json);
                        }
                        promise.complete(response);

                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });
        return promise.future();
    }

    public Future<JsonObject> get(String tableName, List<String> columns, JsonObject filter)
    {
        Promise<JsonObject> promise = Promise.promise();

        String selectClause = columns.isEmpty() ? "*" : String.join(", ", columns);

        var whereClause = new StringBuilder();

        var values = new ArrayList<>();

        filter.forEach(entry ->
        {
            if (!whereClause.isEmpty())
            {
                whereClause.append(" AND ");
            }

            whereClause.append(entry.getKey()).append(" = $").append(values.size() + 1);

            values.add(entry.getValue());
        });

        // Prepare the SQL query
        String query = "SELECT " + selectClause + " FROM " + tableName + " WHERE " + whereClause;

        client.preparedQuery(query)
                .execute(Tuple.from(values), execute ->
                {
                    if (execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        if (rows.iterator().hasNext())
                        {
                            Row row = rows.iterator().next();

                            // Convert the Row into a JsonObject
                            var response = new JsonObject();

                            for (int i = 0; i < row.size(); i++)
                            {
                                var fieldName = row.getColumnName(i);

                                var fieldValue = row.getValue(i);

                                response.put(fieldName, fieldValue);
                            }
                            promise.complete(response);
                        }
                        else
                        {
                            promise.complete(new JsonObject().put("error", "No matching record found"));
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause().getMessage());
                    }
                });

        return promise.future();
    }


    public Future<Boolean> update(String tableName, JsonObject data, JsonObject filter)
    {
        Promise<Boolean> promise = Promise.promise();

        var setClause = new StringBuilder();

        var whereClause = new StringBuilder();

        var values = new ArrayList<>();

        // Construct the SET clause
        data.forEach(entry ->
        {
            if (!setClause.isEmpty())
            {
                setClause.append(", ");
            }

            setClause.append(entry.getKey()).append(" = $").append(values.size() + 1);

            values.add(entry.getValue());
        });

        // Construct the WHERE clause
        filter.forEach(entry ->
        {
            if (!whereClause.isEmpty())
            {
                whereClause.append(" AND ");
            }
            whereClause.append(entry.getKey()).append(" = $").append(values.size() + 1);

            values.add(entry.getValue());
        });

        // Construct the final query
        String query = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;

        // Execute the query
        client.preparedQuery(query)
                .execute(Tuple.from(values), execute ->
                {
                    if (execute.succeeded())
                    {
                        if (execute.result().rowCount() > 0)
                        {
                            promise.complete(true); // Update succeeded
                        }
                        else
                        {
                            promise.complete(false); // No rows updated
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause().getMessage());
                    }
                });

        return promise.future();
    }

}
