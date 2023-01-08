package com.github.zerotobeone.apollo.model;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.github.zerotobeone.apollo.client.ApolloClient;
import com.github.zerotobeone.apollo.util.FileUtil;
import com.github.zerotobeone.apollo.util.IgnoreUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublishTask {

    private static final Log log = LogFactory.get();

    private final List<ApolloFileModel> apolloFileModels;

    public PublishTask(ApolloFileModel apolloFileModel) {
        this.apolloFileModels = Collections.singletonList(apolloFileModel);
    }

    public PublishTask(List<ApolloFileModel> apolloFileModels) {
        this.apolloFileModels = apolloFileModels;
    }

    public PublishTask(String publishEnv, String rootUrl) {
        this.apolloFileModels = Lists.newArrayList();
        File file = new File(rootUrl);
        File[] appFiles = file.listFiles();
        if (appFiles == null) {
            log.error("【error folder】{}", rootUrl);
            return;
        }
        for(File appFile : appFiles){
            String appId = appFile.getName().split("&")[0];
            if (IgnoreUtil.isIgnored(appId)) {
                continue;
            }
            File[] envFiles = appFile.listFiles();
            if (envFiles == null) {
                log.error("【error folder】{}", appId);
                continue;
            }
            for(File envFile : envFiles){
                String env = envFile.getName();

                File[] clusterFiles = envFile.listFiles();
                if (clusterFiles == null) {
                    log.error("【error folder】{}", appId);
                    continue;
                }

                for(File clusterFile : clusterFiles){
                    String cluster = clusterFile.getName();
                    File[] namespaceFiles = clusterFile.listFiles();
                    if (namespaceFiles == null){
                        log.error("【error folder】{}", appId);
                        continue;
                    }

                    for (File namespacefs : namespaceFiles) {
                        String namespace = namespacefs.getName().replace(".properties", "");
                        if(publishEnv.equals(env)){
                            ApolloFileModel apolloFileModel = new ApolloFileModel();
                            apolloFileModel.setAppId(appId);
                            apolloFileModel.setFile(namespacefs);
                            apolloFileModel.setEnv(publishEnv);
                            apolloFileModel.setNamespace(namespace);
                            apolloFileModel.setCluster(cluster);
                            this.apolloFileModels.add(apolloFileModel);
                        }
                    }
                }
            }
        }
    }

    public void publishSync(){
        this.apolloFileModels.forEach(apolloFileModel -> fastPublish(apolloFileModel.getAppId(), apolloFileModel.getEnv(), apolloFileModel.getCluster(), apolloFileModel.getNamespace(), apolloFileModel.getFile()));
    }

    public void publishAsync(int threadNumber){
        ExecutorService executors = Executors.newFixedThreadPool(threadNumber);
        CountDownLatch countDownLatch = new CountDownLatch(this.apolloFileModels.size());
        this.apolloFileModels.forEach(apolloFileModel -> executors.submit(() -> {
            try {
                fastPublish(apolloFileModel.getAppId(), apolloFileModel.getEnv(), apolloFileModel.getCluster(), apolloFileModel.getNamespace(), apolloFileModel.getFile());
            } catch (Exception e) {
                log.error(e);
            }finally {
                countDownLatch.countDown();
                log.info("【remaining】{} of {}", countDownLatch.getCount(), this.apolloFileModels.size());
            }
        }));
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executors.shutdown();
    }

    private static void fastPublish(String appId, String env, String clusterName, String namespaceName, File namespaceFile) {
        Map<String, String> oldProperties = ApolloClient.checkAndGetNamespaceProperties(appId, env, clusterName, namespaceName);
        //read the file
        Map<String, String> newProperties = FileUtil.readPropertiesFile(namespaceFile.getAbsolutePath());

        //compare old value
        newProperties.keySet().forEach(key ->{
            String newValue = newProperties.get(key);
            String oldValue = oldProperties.get(key);
            if (newValue != null && newValue.equals(oldValue)){
                newProperties.remove(key);
            }
            oldProperties.remove(key);
        });

        //create or update new value
        newProperties.keySet().forEach(key ->{
            if(!Strings.isNullOrEmpty(key)){
                String value = newProperties.get(key);
                ApolloClient.createOrUpdate(appId, env, clusterName, namespaceName, key, value);
            }
        });

        //delete key
        oldProperties.keySet().forEach(key ->{
            if(key != null && !"".equals(key)){
                ApolloClient.removeItem(appId, env, clusterName, namespaceName, key);
            }
        });

        boolean needToPublished = CollectionUtil.isNotEmpty(newProperties) || CollectionUtil.isNotEmpty(oldProperties);
        if(needToPublished){
            ApolloClient.publish(appId, env, clusterName, namespaceName);
        }else {
            log.info("【publish no need】{}/{}/{}", appId, env, namespaceName);
        }
    }

}
