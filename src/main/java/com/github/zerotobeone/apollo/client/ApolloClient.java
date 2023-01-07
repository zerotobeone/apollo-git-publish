package com.github.zerotobeone.apollo.client;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.github.zerotobeone.apollo.conf.ApolloConf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ApolloClient
 *
 * @author zerotobeone
 * @version ApolloClient.java v1.0 2023-01-05
 */
public class ApolloClient {
    private static final Log log = LogFactory.get();

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

    public static void removeItem(String appId, String env, String clusterName, String namespaceName, String key) {
        try{
            getClient().removeItem(appId, env, clusterName, namespaceName, key, ApolloConf.releaseBy);
            log.debug("【delete】{}/{}/{}/ {}", appId, env, namespaceName, key);
        }catch (Exception e){
            log.error("【delete exception】{}/{}/{}/ {}", appId, env, namespaceName, key);
        }
    }

    public static void createOrUpdate(String appId, String env, String clusterName, String namespaceName, String key, String value) {
        OpenItemDTO openItemDTO = new OpenItemDTO();
        openItemDTO.setKey(key);
        openItemDTO.setValue(value);
        openItemDTO.setDataChangeCreatedBy(ApolloConf.releaseBy);
        try{
            getClient().createOrUpdateItem(appId, env, clusterName, namespaceName, openItemDTO);
            log.debug("【update】{}/{}/{}/ {} : {}", appId, env, namespaceName, key, value);
        }catch (Exception e){
            log.error("【update exception】{}/{}/{}/ {} : {}", appId, env, namespaceName, key, value);
            log.error(e, "【need to check permission】{}", appId);
        }
    }

    public static void publish(String appId, String env, String clusterName, String namespaceName) {
        NamespaceReleaseDTO namespaceReleaseDTO  = new NamespaceReleaseDTO();
        namespaceReleaseDTO.setReleasedBy(ApolloConf.releaseBy);
        namespaceReleaseDTO.setReleaseComment(ApolloConf.releaseComment);
        namespaceReleaseDTO.setReleaseTitle(ApolloConf.releaseTitle);
        try {
            ApolloClient.getClient().publishNamespace(appId, env, clusterName, namespaceName, namespaceReleaseDTO);
            log.info("【publish】{}/{}/{}", appId, env, namespaceName);
        }catch (Exception e){
            log.error(e, "【publish error】{}/{}/{}", appId, env, namespaceName);
        }
    }

    public static Map<String, String> checkAndGetNamespaceProperties(String appId, String env, String clusterName, String namespaceName){
        try{
            OpenNamespaceDTO openNamespaceDTO = ApolloClient.getClient().getNamespace(appId, env, clusterName, namespaceName);
            Map<String, String> properties = new ConcurrentHashMap<>();
            openNamespaceDTO.getItems().forEach(item -> properties.put(item.getKey(), item.getValue()));
            return properties;
        } catch (Exception e){
            log.error("【namespace check error】{}/{}/{}/{}",appId, env, clusterName, namespaceName);
            throw e;
        }
    }

}
