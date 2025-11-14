package db.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

public class ElasticsearchUtil {

    private static ElasticsearchClient client;

    public static void init() {
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
                .build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        client = new ElasticsearchClient(transport);
    }

    public static ElasticsearchClient getClient() {
        if (client == null) {
            init();
        }
        return client;
    }

    public static void close() throws Exception {
        if (client != null) {
            client._transport().close();
        }
    }
}