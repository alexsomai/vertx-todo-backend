package io.vertx.todo;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;

import static io.vertx.todo.matcher.VertxMatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/*
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
@RunWith(VertxUnitRunner.class)
public class ServerTest {

    private static MongodProcess mongo;
    private static final int MONGO_PORT = 12345;

    private Vertx vertx;

    @BeforeClass
    public static void initialize() throws IOException {
        MongodStarter starter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                .build();
        MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
        mongo = mongodExecutable.start();
    }

    @AfterClass
    public static void shutdown() {
        mongo.stop();
    }

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
                    assertThat(context, body.toString(), is(not("")));
                    context.assertFalse(body.toString().isEmpty());
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
                        assertThat(context, jsonObject.getString("title"), is("title"));
                        assertThat(context, jsonObject.getBoolean("completed"), is(false));
                        assertThat(context, jsonObject.getString("url"), is(not("")));
                        async.complete();
                    }));
        request.end(new JsonObject().put("title", "title").encodePrettily());
    }
}