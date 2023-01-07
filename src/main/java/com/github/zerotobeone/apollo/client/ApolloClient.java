package com.github.zerotobeone.apollo.client;

import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.github.zerotobeone.apollo.conf.ApolloConf;

/**
 * ApolloClient
 *
 * @author zerotobeone
 * @version ApolloClient.java v1.0 2023-01-05
 */
public class ApolloClient {
    private static class ApolloClientHolder{
        private static final ApolloOpenApiClient INSTANCE = ApolloOpenApiClient.newBuilder()
                .withPortalUrl(ApolloConf.url)
                .withToken(ApolloConf.token)
                .build();
    }

    private ApolloClient(){}

    public static ApolloOpenApiClient getClient() {
       return ApolloClientHolder.INSTANCE;
    }
}
