package com.github.zerotobeone.apollo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.github.zerotobeone.apollo.conf.ApolloConf;
import com.github.zerotobeone.apollo.conf.FileConf;
import com.github.zerotobeone.apollo.util.ConsoleUtil;
import com.github.zerotobeone.apollo.util.IgnoreUtil;
import com.google.common.base.Strings;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    public static void main(String[] args) throws Exception{
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
        File[] appfss = file.listFiles();
        for(File appfs : appfss){
            String appid = appfs.getName().split("&")[0];
            if (IgnoreUtil.isIgnored(appid)) {
                continue;
            }
            File[] envfss = appfs.listFiles();
            if (envfss == null) {
                log.error("【error folder】{}", appid);
                continue;
            }
            for(File envfs : envfss){
                String env = envfs.getName();

                File[] clusterfss = envfs.listFiles();
                if (clusterfss == null) {
                    log.error("【error folder】{}", appid);
                    continue;
                }

                for(File clusterfs : clusterfss){
                    String cluster = clusterfs.getName();
                    File[] namespacefss = clusterfs.listFiles();
                    if (namespacefss == null){
                        log.error("【error folder】{}", appid);
                        continue;
                    }

                    for (File namespacefs : namespacefss) {
                        String namespace = namespacefs.getName().replace(".properties", "");

                        //deploy
                        if(deployEnv.equals(env)){
                            executors.submit(() -> {
                                try {
                                    fastDeploy(appid, env, cluster, namespace, namespacefs);
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

    private static Map<String, String> checkAndGetNamespace(String appid, String env, String clusterName, String namespaceName){
        try{
            OpenNamespaceDTO openNamespaceDTO = ApolloClient.getClient().getNamespace(appid, env, clusterName, namespaceName);
            Map<String, String> properties = new ConcurrentHashMap<>();
            openNamespaceDTO.getItems().forEach(item -> properties.put(item.getKey(), item.getValue()));
            return properties;
        } catch (Exception e){
            log.error("【namespace check error】{}/{}/{}/{}",appid, env, clusterName, namespaceName);
            throw e;
        }
    }

    private static int countNum(String deployEnv, String path){
        int taskCount = 0;

        File file = new File(path);
        File[] appfss = file.listFiles();
        for(File appfs : appfss){
            String appid = appfs.getName().split("&")[0];
            if (IgnoreUtil.isIgnored(appid)) {
                continue;
            }
            File[] envfss = appfs.listFiles();
            if (envfss == null) {
                continue;
            }
            for(File envfs : envfss){
                String env = envfs.getName();

                File[] clusterfss = envfs.listFiles();
                if (clusterfss == null) {
                    continue;
                }

                for(File clusterfs : clusterfss){
                    File[] namespacefss = clusterfs.listFiles();
                    if (namespacefss == null){
                        continue;
                    }

                    if(deployEnv.equals(env)){
                        taskCount += namespacefss.length;
                    }
                }
            }
        }
        return taskCount;
    }

    private static void fastDeploy(String appid, String env, String clusterName, String namespaceName, File namespaceFile) {
        Map<String, String> oldProperties = checkAndGetNamespace(appid, env, clusterName, namespaceName);
        //read the file
        Map<String, String> newProperties = new ConcurrentHashMap<>();
        FileReader fileReader = new FileReader(namespaceFile.getAbsolutePath(),  "UTF-8");
        List<String> result = fileReader.readLines();
        for (String line : result) {
            //jump over comment line
            if(line.trim().startsWith("#")) {
                continue;
            }
            //jump over empty line
            if("".equals(line.trim())) {
                continue;
            }
            //get the key and value of one line
            int splitIndex = line.indexOf("=");
            String key = line.substring(0 , splitIndex).trim();
            String value = line.substring(splitIndex + 1).trim().replace("\\n","\n");

            if(!Strings.isNullOrEmpty(key)){
               newProperties.put(key, value);
            }
        }

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
                OpenItemDTO openItemDTO = new OpenItemDTO();
                openItemDTO.setKey(key);
                openItemDTO.setValue(value);
                openItemDTO.setDataChangeCreatedBy(ApolloConf.releaseBy);
                try{
                    ApolloClient.getClient().createOrUpdateItem(appid, env, clusterName, namespaceName, openItemDTO);
                    log.debug("【update】{}/{}/{}/ {} : {}",appid, env, namespaceName, key, value);
                }catch (Exception e){
                    log.error("【update exception】{}/{}/{}/ {} : {}",appid, env, namespaceName, key, value);
                    log.error(e, "【need to check permission】{}", appid);
                }
            }
        });

        //delete key
        oldProperties.keySet().forEach(key ->{
            if(key != null && !"".equals(key)){
                try{
                    ApolloClient.getClient().removeItem(appid, env, clusterName, namespaceName, key, "apollo");
                    log.debug("【delete】{}/{}/{}/ {}",appid, env, namespaceName, key);
                }catch (Exception e){
                    log.error("【delete exception】{}/{}/{}/ {}",appid, env, namespaceName, key);
                }
            }
        });

        boolean needToPublished = CollectionUtil.isNotEmpty(newProperties) || CollectionUtil.isNotEmpty(oldProperties);
        if(needToPublished){
            NamespaceReleaseDTO namespaceReleaseDTO  = new NamespaceReleaseDTO();
            namespaceReleaseDTO.setReleasedBy(ApolloConf.releaseBy);
            namespaceReleaseDTO.setReleaseComment(ApolloConf.releaseComment);
            namespaceReleaseDTO.setReleaseTitle(ApolloConf.releaseTitle);
            try {
                ApolloClient.getClient().publishNamespace(appid, env, clusterName, namespaceName, namespaceReleaseDTO);
            }catch (Exception e){
                log.error(e, "【publish error】{}/{}/{}", appid, env, namespaceName);
            }
            log.info("【publish】{}/{}/{}", appid, env, namespaceName);
        }else {
            log.info("【publish no need】{}/{}/{}", appid, env, namespaceName);
        }

    }
}
