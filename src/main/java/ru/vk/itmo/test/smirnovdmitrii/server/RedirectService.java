package ru.vk.itmo.test.smirnovdmitrii.server;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import ru.vk.itmo.test.smirnovdmitrii.application.properties.DhtValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedirectService {

    @DhtValue("cluster.redirect.timeout:100")
    private static int REDIRECT_TIMEOUT;

    private final Map<String, HttpClient> clients = new HashMap<>();

    public RedirectService(final String selfUrl, List<String> clusterUrls) {
        for (final String clusterUrl: clusterUrls) {
            if (clusterUrl.equals(selfUrl)) {
                continue;
            }
            clients.put(clusterUrl, new HttpClient(new ConnectionString(clusterUrl)));
        }
    }

    public Response redirect(
            final String url,
            final Request request
    ) throws HttpException, IOException, InterruptedException {
        final HttpClient client = clients.get(url);
        final Request newRequest = client.createRequest(request.getMethod(), url, request.getHeaders());
        newRequest.setBody(request.getBody());
        try {
            return client.invoke(request, REDIRECT_TIMEOUT);
        } catch (PoolException e) {
            return new Response(Response.BAD_GATEWAY, Response.EMPTY);
        }
    }

    public void close() {
        for (final Map.Entry<String, HttpClient> entry: clients.entrySet()) {
            entry.getValue().close();
        }
    }
}
