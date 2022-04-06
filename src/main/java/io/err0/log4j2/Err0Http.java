package io.err0.log4j2;

import com.google.gson.JsonObject;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Factory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.ssl.TlsDetails;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class Err0Http {
    final static CloseableHttpAsyncClient client;
    static {
        final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .useSystemProperties()
                // IMPORTANT uncomment the following method when running Java 9 or older
                // in order for ALPN support to work and avoid the illegal reflective
                // access operation warning
                .setTlsDetailsFactory(new Factory<SSLEngine, TlsDetails>() {
                    @Override
                    public TlsDetails create(final SSLEngine sslEngine) {
                        return new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol());
                    }
                })
                .build();
        final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .build();
        client = HttpAsyncClients.custom()
                .setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                .setConnectionManager(cm)
                .build();

        client.start();
    }
    final static AtomicInteger inFlight = new AtomicInteger(0);
    public static boolean canCall() {
        return inFlight.get() < 4;
    }
    public static void call(final URL url, final String token, final JsonObject payload)
    {
        inFlight.incrementAndGet();
        try {
            final HttpHost target = new HttpHost(url.getProtocol(), url.getHost(), url.getPort());
            final HttpClientContext clientContext = HttpClientContext.create();
            final SimpleHttpRequest request = SimpleRequestBuilder.post()
                    .setHttpHost(target)
                    .setPath(url.getPath())
                    .setHeader("Authorization", "Bearer " + token)
                    .setBody(payload.toString(), ContentType.APPLICATION_JSON)
                    .build();

            System.out.println("Executing request " + request);
            final Future<SimpleHttpResponse> future = client.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    clientContext,
                    new FutureCallback<SimpleHttpResponse>() {

                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            inFlight.decrementAndGet();
                            System.out.println(request + "->" + new StatusLine(response));
                            final SSLSession sslSession = clientContext.getSSLSession();
                            if (sslSession != null) {
                                System.out.println("SSL protocol " + sslSession.getProtocol());
                                System.out.println("SSL cipher suite " + sslSession.getCipherSuite());
                            }
                            System.out.println(response.getBody());
                        }

                        @Override
                        public void failed(final Exception ex) {
                            inFlight.decrementAndGet();
                            System.out.println(request + "->" + ex);
                        }

                        @Override
                        public void cancelled() {
                            inFlight.decrementAndGet();
                            System.out.println(request + " cancelled");
                        }

                    });
        }
        catch (Exception ex) {

        }
    }

    public static void shutdown() {
        System.out.println("Shutting down");
        client.close(CloseMode.GRACEFUL);
    }
}
