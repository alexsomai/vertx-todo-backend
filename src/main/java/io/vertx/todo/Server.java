package io.vertx.todo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/*
 * @author <a href="mailto:somai.alexandru@gmail.com">Alexandru Somai</a>
 */
public class Server extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static final String TODOS_COLLECTION = "todos";

    private Router router;
    private MongoClient mongo;

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Launcher.main(new String[]{"run", Server.class.getName()});
    }

    @Override
    public void start(Future<Void> future) {
        router = Router.router(vertx);
        configMongo();
        setUpCors();
        setUpRoutes();
        createHttpServer(future);
    }

    private void configMongo() {
        String dbName = "demo";
        String uri = "mongodb://localhost:27017";

        // needed to be able to run the app on https://openshift.redhat.com
        String mongoDbUrl = System.getenv("OPENSHIFT_MONGODB_DB_URL");
        if (mongoDbUrl != null) {
            uri = mongoDbUrl;
            dbName = System.getenv("OPENSHIFT_APP_NAME");
        }

        JsonObject mongoConfig = new JsonObject()
                .put("connection_string", uri)
                .put("db_name", dbName);

        mongo = MongoClient.createShared(vertx, mongoConfig);
    }

    private void setUpCors() {
        router.route().handler(BodyHandler.create());
        router.route().handler((routingContext) -> {
            routingContext.response()
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH")
                    .putHeader("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept")
                    .putHeader("Access-Control-Max-Age", "3600");
            routingContext.next();
        });
    }

    private void setUpRoutes() {
        router.get("/").handler((context) -> context.reroute("/todos"));
        router.get("/todos").handler(this::getAllTodos);
        router.get("/todos/:todoId").handler(this::getTodo);
        router.post("/todos").handler(this::addTodo);
        router.patch("/todos/:todoId").handler(this::updateTodo);
        router.put("/todos/:todoId").handler(this::updateTodo);
        router.delete("/todos/:todoId").handler(this::deleteTodo);
        router.delete("/todos/").handler(this::deleteAllTodos);
        router.options("/todos").handler((handler) -> handler.response().end());
        router.options("/todos/:todoId").handler((handler) -> handler.response().end());
    }

    private void createHttpServer(Future<Void> future) {
        Integer port = 8080;
        String host = "localhost";

        // needed to be able to run the app on https://openshift.redhat.com
        final String openShiftVertxIP = System.getenv("OPENSHIFT_VERTX_IP");
        if (openShiftVertxIP != null) {
            port = Integer.getInteger("http.port");
            host = System.getProperty("http.address");
        }

        vertx.createHttpServer().requestHandler(router::accept).listen(port, host, result -> {
            if (result.succeeded()) {
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });
    }

    private void getAllTodos(RoutingContext routingContext) {
        mongo.find(TODOS_COLLECTION, new JsonObject(), results -> {
            if (results.succeeded()) {
                JsonArray arr = new JsonArray();
                results.result().forEach(arr::add);
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(arr.encodePrettily());
            }
            if (results.failed()) {
                sendError(500, routingContext.response(), results.cause());
            }
        });
    }

    private void getTodo(RoutingContext routingContext) {
        String id = routingContext.request().getParam("todoId");
        if (id == null) {
            sendError(400, routingContext.response());
        } else {
            mongo.findOne(TODOS_COLLECTION, new JsonObject().put("_id", id), new JsonObject(), result -> {
                if (result.succeeded()) {
                    if (result.result() == null) {
                        sendError(404, routingContext.response());
                    } else
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(result.result().encodePrettily());
                }
                if (result.failed()) {
                    sendError(500, routingContext.response(), result.cause());
                }
            });
        }
    }

    private void addTodo(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        JsonObject todo = routingContext.getBodyAsJson();
        if (todo == null) {
            sendError(400, response);
        } else {
            mongo.insert(TODOS_COLLECTION, todo, insertResult -> {
                if (insertResult.succeeded()) {
                    String id = insertResult.result();
                    todo.put("_id", id);
                    todo.put("url", routingContext.request().absoluteURI() + "/" + id);
                    todo.put("completed", false);

                    mongo.save(TODOS_COLLECTION,
                            todo,
                            updateResult -> {
                                if (updateResult.succeeded()) {
                                    response.setStatusCode(201)
                                            .putHeader("content-type", "application/json; charset=utf-8")
                                            .end(todo.encodePrettily());
                                }
                                if (updateResult.failed()) {
                                    sendError(500, routingContext.response(), updateResult.cause());
                                }
                            });
                }
                if (insertResult.failed()) {
                    sendError(500, routingContext.response(), insertResult.cause());
                }
            });
        }
    }

    private void updateTodo(RoutingContext routingContext) {
        String id = routingContext.request().getParam("todoId");
        JsonObject jsonObject = routingContext.getBodyAsJson();
        if (id == null || jsonObject == null) {
            sendError(400, routingContext.response());
        } else {
            mongo.update(TODOS_COLLECTION,
                    new JsonObject().put("_id", id),
                    new JsonObject().put("$set", jsonObject),
                    result -> {
                        if (result.succeeded()) {
                            getTodo(routingContext);
                        }
                        if (result.failed()) {
                            sendError(500, routingContext.response(), result.cause());
                        }
                    });
        }
    }

    private void deleteTodo(RoutingContext routingContext) {
        String id = routingContext.request().getParam("todoId");
        if (id == null) {
            sendError(400, routingContext.response());
        } else {
            mongo.removeOne(TODOS_COLLECTION, new JsonObject().put("_id", id), result -> {
                if (result.succeeded()) {
                    routingContext.response().end();
                }
                if (result.failed()) {
                    sendError(500, routingContext.response(), result.cause());
                }
            });
        }
    }

    private void deleteAllTodos(RoutingContext routingContext) {
        mongo.dropCollection(TODOS_COLLECTION, result -> {
            if (result.succeeded()) {
                routingContext.response().end();
            }
            if (result.failed()) {
                sendError(500, routingContext.response(), result.cause());
            }
        });
    }

    private static void sendError(int statusCode, HttpServerResponse response) {
        sendError(statusCode, response, null);
    }

    private static void sendError(int statusCode, HttpServerResponse response, Throwable cause) {
        if (cause != null) {
            LOGGER.error(cause.getMessage(), cause);
        }
        response.setStatusCode(statusCode).end();
    }
}