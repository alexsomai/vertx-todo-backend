package io.vertx.todo;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
@RunWith(VertxUnitRunner.class)
@Ignore
public class ServerTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(Server.class.getName(), context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testGetAllTodos(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(8080, "localhost", "/todos",
                response -> response.handler(body -> {
                    context.assertEquals(body.toJsonArray(), new JsonArray());
                    async.complete();
                }));
    }

    @Test
    public void testAddTodo(TestContext context) {
        final Async async = context.async();

        HttpClientRequest request = vertx.createHttpClient().post(8080, "localhost", "/todos",
                response ->
                    response.handler(body -> {
                        JsonObject jsonObject = body.toJsonObject();
                        context.assertEquals(jsonObject.getString("title"), "title");
                        context.assertEquals(jsonObject.getBoolean("completed"), false);
                        context.assertTrue(jsonObject.containsKey("url"));
                        async.complete();
                    }));
        request.end(new JsonObject().put("title", "title").encodePrettily());
    }
}