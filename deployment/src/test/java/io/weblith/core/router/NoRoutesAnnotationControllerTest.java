package io.weblith.core.router;

import static io.restassured.RestAssured.when;

import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.weblith.core.router.annotations.Controller;

public class NoRoutesAnnotationControllerTest {

    private final static int NOT_FOUND = Status.NOT_FOUND.getStatusCode();

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(NoRoutesAnnotationWeblithController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(new StringAsset("quarkus.http.test-port=0"), "application.properties"));

    @Controller
    public class NoRoutesAnnotationWeblithController {

        // Explicitly wrong annotation
        @javax.ws.rs.GET
        public String get() {
            return "ignored";
        }

    }

    @Test
    public void testWeblithIgnoredResource() {
        when().get("/ignored").then().statusCode(NOT_FOUND);
        when().get("/MissingAnnotationWeblith/ignored").then().statusCode(NOT_FOUND);
        when().get("/MissingAnnotationWeblithController/ignored").then().statusCode(NOT_FOUND);
    }

}
