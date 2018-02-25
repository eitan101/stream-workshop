package grp;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static grp.utils.JDBIUtils.prepareDao;
import static grp.utils.PubSub.DefaultSubscriber.subscriber;
import static pl.touk.throwing.ThrowingRunnable.unchecked;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public class MyApp extends Application<Configuration> {

    public static final String JDBC_USER = System.getenv().getOrDefault("JDBC_USER", "root");
    public static final String JDBC_PASS = System.getenv().getOrDefault("JDBC_PASS", "root");
    public static final String JDBC_URL = System.getenv().getOrDefault("JDBC_URL", "jdbc:h2:mem:mytable");
    public static final String JDBC_DRIVER = System.getenv().getOrDefault("JDBC_DRIVER", "org.h2.Driver");

    public static void main(String[] args) throws Exception {
        new MyApp().run(args);
    }
    static MyDAO myDAO;
    static Date lastMofified = new Date();
    static SubmissionPublisher<Date> UpdateNotifications = new SubmissionPublisher<>();
    static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/"));

    }

    @Override
    public void run(Configuration configuration, Environment environment) {

        myDAO = prepareDao(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASS, environment, MyDAO.class);
        myDAO.createSomethingTable();
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new RestResource());

    }

    @Timed
    public interface MyDAO {
        @SqlUpdate("create table if not exists something (id varchar(100) primary key, name varchar(1000))")
        void createSomethingTable();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") String id, @Bind("name") JsonNode name);

        @SqlQuery("select name from something limit :limit")
        List<JsonNode> getAll(@Bind("limit") int limit);
    }


    @Path("/items")
    @Timed
    public static class RestResource {
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public Response post(ObjectNode value) {
            final String id = UUID.randomUUID().toString();
            value.put("id", id);
            myDAO.insert(id, value);
            UpdateNotifications.submit(lastMofified = new Date());
            return Response.created(URI.create("/api/items/" + id)).build();
        }

        @GET
        @Produces(SseFeature.SERVER_SENT_EVENTS)
        public EventOutput getServerSentEvents() throws IOException {
            final EventOutput sseEndpoint = new EventOutput();
            scheduler.schedule(unchecked(() ->
                    sseEndpoint.write(new OutboundEvent.Builder().name("message")
                        .data(String.class, myDAO.getAll(50).toString()).build())),
                    1000, TimeUnit.MILLISECONDS);
            UpdateNotifications.subscribe(subscriber(date ->
                    sseEndpoint.write(new OutboundEvent.Builder().name("message")
                            .data(String.class, myDAO.getAll(50).toString()).build())));
            return sseEndpoint;
        }
    }
}
