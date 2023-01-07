package com.github.zerotobeone.apollo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.github.zerotobeone.apollo.client.ApolloClient;
import com.github.zerotobeone.apollo.conf.FileConf;
import com.github.zerotobeone.apollo.util.ConsoleUtil;
import com.github.zerotobeone.apollo.util.FileUtil;
import com.github.zerotobeone.apollo.util.IgnoreUtil;
import com.google.common.base.Strings;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PublishToApollo
 *
 * @author zerotobeone
 * @version deploy.java v1.0 2022-12-01
 */
public class PublishToApollo {
    private static final Log log = LogFactory.get();

    public static void main(String[] args) {
        ConsoleUtil.logApplicationTitle();
        TimeInterval timer = DateUtil.timer();
        String appId;
        String deployEnv = ConsoleUtil.getInput("please input deploy env: [DEV]", "DEV");
        String deployType = ConsoleUtil.getInput("please input deploy type (1.all, 2.single app, 3.single namespace): [1]", "1");
        switch (deployType){
            case "1":
                // all
                String threadNum = ConsoleUtil.getInput("please input thread num: [20]", "20");
                log.info("【start】");
                timer.start("deploy");
                deployAll(deployEnv, Integer.parseInt(threadNum));
                break;
            case "2":
                // single app
                appId = ConsoleUtil.getInput("please input appId:");
                log.info("【start】");
                timer.start("deploy");
                deploySingleApp(deployEnv, appId);
                break;
            case "3":
                // single namespace
                appId = ConsoleUtil.getInput("please input appId:");
                String cluster = ConsoleUtil.getInput("please input cluster: [default]", "default");
                String namespace = ConsoleUtil.getInput("please input namespace: [application]", "application");
                log.info("【start】");
                timer.start("deploy");
                deploySingleAppNameSpace(deployEnv, appId, namespace, cluster);
                break;
            default:
                Console.log("deploy input error");
                break;
        }
        log.info("【done】");
        Console.log("deploy took {} ms", timer.intervalMs("deploy"));
    }

    private static void deploySingleApp(final String deployEnv, final String appId) {
        String path = FileConf.fileRootUrl + appId + "/" + deployEnv;
        File envFile = new File(path);
        File[] clusterFiles = envFile.listFiles();
        if (clusterFiles == null) {
            log.error("【error folder】{}", appId);
            return;
        }

        for(File clusterFile : clusterFiles){
            String cluster = clusterFile.getName();
            File[] namespaceFiles = clusterFile.listFiles();
            if (namespaceFiles == null){
                log.error("【error folder】{}", appId);
                continue;
            }

            for (File namespaceFile : namespaceFiles) {
                String namespace = namespaceFile.getName().replace(".properties", "");
                //deploy
                fastDeploy(appId, deployEnv, cluster, namespace, namespaceFile);
            }
        }
    }

    private static void deploySingleAppNameSpace(final String deployEnv, final String appId, final String namespace, final String cluster) {
        String path = FileConf.fileRootUrl + appId + "/" + deployEnv + "/" + cluster + "/" + namespace + ".properties";
        File file = new File(path);
        fastDeploy(appId, deployEnv, cluster, namespace, file);
    }

    private static void deployAll(String deployEnv, int threadNum) {
        ExecutorService executors = Executors.newFixedThreadPool(threadNum);
        String path = FileConf.fileRootUrl;
        int taskCount = countNum(deployEnv, path);
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);
        File file = new File(path);
        File[] appFiles = file.listFiles();
        if (appFiles == null) {
            log.error("【error folder】{}", path);
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

                        //deploy
                        if(deployEnv.equals(env)){
                            executors.submit(() -> {
                                try {
                                    fastDeploy(appId, env, cluster, namespace, namespacefs);
                                } catch (Exception e) {
                                    log.error(e);
                                }finally {
                                    countDownLatch.countDown();
                                    log.info("【remaining】{} of {}", countDownLatch.getCount(), taskCount);
                                }
                            });
                        }
                    }
                }
            }
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executors.shutdown();
    }

    private static int countNum(String deployEnv, String path){
        int taskCount = 0;

        File file = new File(path);
        File[] appFiles = file.listFiles();
        if (appFiles == null) {
            log.error("【error folder】{}", path);
            return 0;
        }
        for(File appFile : appFiles){
            String appId = appFile.getName().split("&")[0];
            if (IgnoreUtil.isIgnored(appId)) {
                continue;
            }
            File[] envFiles = appFile.listFiles();
            if (envFiles == null) {
                continue;
            }
            for(File envFile : envFiles){
                String env = envFile.getName();

                File[] clusterFiles = envFile.listFiles();
                if (clusterFiles == null) {
                    continue;
                }

                for(File clusterFile : clusterFiles){
                    File[] namespaceFiles = clusterFile.listFiles();
                    if (namespaceFiles == null){
                        continue;
                    }

                    if(deployEnv.equals(env)){
                        taskCount += namespaceFiles.length;
                    }
                }
            }
        }
        return taskCount;
    }

    private static void fastDeploy(String appId, String env, String clusterName, String namespaceName, File namespaceFile) {
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
