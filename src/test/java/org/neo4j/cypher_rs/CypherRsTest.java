package org.neo4j.cypher_rs;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Hunger @since 09.10.13
 */
public class CypherRsTest extends RestTestBase {

    public static final String KEY = "foo";
    public static final String WRITE_KEY = "bar";
    private WebResource cypherRsPath;
    private WebResource cypherRsWritePath;
    public static final String QUERY = "start n=node({id}) return n";
    public static final String WRITE_QUERY = "start n=node({id}) delete n";
    private static final String PRETTY_QUERY = "START n=node({ id })\nRETURN n";
    private static final String PRETTY_WRITE_QUERY = "START n=node({ id })\nDELETE n";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        WebResource path = rootResource.path("test");
        cypherRsPath = path.path(KEY);
        cypherRsWritePath = path.path(WRITE_KEY);
    }

    @Test
    public void testAddEndpoint() throws Exception {
        ClientResponse response = cypherRsPath.put(ClientResponse.class, QUERY);
        assertEquals(201, response.getStatus());
        assertEquals(cypherRsPath.getURI(),response.getLocation());
        try (Transaction tx = beginTx()) {
            assertEquals(QUERY, properties().getProperty(KEY));
            tx.success();
        }
    }
    @Test
    public void testDeleteNonExistingEndpoint() throws Exception {
        ClientResponse response = cypherRsPath.delete(ClientResponse.class);
        assertEquals(404, response.getStatus());
    }
    @Test
    public void testDeleteExistingEndpoint() throws Exception {
        cypherRsPath.put(ClientResponse.class, QUERY);
        ClientResponse response = cypherRsPath.delete(ClientResponse.class);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testShowEndpoint() throws Exception {
        cypherRsPath.put(ClientResponse.class, QUERY);
        ClientResponse response = cypherRsPath.path("_show").get(ClientResponse.class);
        assertEquals(200, response.getStatus());
        Map<String, Object> resultJson = Utils.readJson(response.getEntity(String.class));
        assertEquals(PRETTY_QUERY, resultJson.get("query"));
        assertEquals(false, resultJson.get("is_write_query"));
    }

    @Test
    public void testShowAllEndpoints() throws Exception {
        cypherRsPath.put(ClientResponse.class, QUERY);
        cypherRsWritePath.put(ClientResponse.class, WRITE_QUERY);
        ClientResponse response = rootResource.path("test").get(ClientResponse.class);
        assertEquals(200, response.getStatus());
        Map<String, Object> resultJson = Utils.readJson(response.getEntity(String.class));
        assertEquals(2, resultJson.size());

        Map<String, Object> query;
        query = (Map<String, Object>) resultJson.get(KEY);
        assertEquals(PRETTY_QUERY, query.get("query"));
        assertEquals(false, query.get("is_write_query"));

        query = (Map<String, Object>) resultJson.get(WRITE_KEY);
        assertEquals(PRETTY_WRITE_QUERY, query.get("query"));
        assertEquals(true, query.get("is_write_query"));
    }
}
