package com.github.zerotobeone.apollo;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.github.zerotobeone.apollo.conf.FileConf;
import com.github.zerotobeone.apollo.model.ApolloFileModel;
import com.github.zerotobeone.apollo.model.PublishTask;
import com.github.zerotobeone.apollo.util.ConsoleUtil;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

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
        List<ApolloFileModel> apolloFileModelList = Lists.newLinkedList();
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
                ApolloFileModel apolloFileModel = new ApolloFileModel();
                apolloFileModel.setAppId(appId);
                apolloFileModel.setFile(namespaceFile);
                apolloFileModel.setEnv(deployEnv);
                apolloFileModel.setNamespace(namespace);
                apolloFileModel.setCluster(cluster);
                apolloFileModelList.add(apolloFileModel);
            }
        }

        PublishTask publishTask = new PublishTask(apolloFileModelList);
        publishTask.publishSync();
    }

    private static void deploySingleAppNameSpace(final String deployEnv, final String appId, final String namespace, final String cluster) {
        String path = FileConf.fileRootUrl + appId + "/" + deployEnv + "/" + cluster + "/" + namespace + ".properties";
        File file = new File(path);
        ApolloFileModel apolloFileModel = new ApolloFileModel();
        apolloFileModel.setAppId(appId);
        apolloFileModel.setFile(file);
        apolloFileModel.setEnv(deployEnv);
        apolloFileModel.setNamespace(namespace);
        apolloFileModel.setCluster(cluster);
        PublishTask publishTask = new PublishTask(apolloFileModel);
        publishTask.publishSync();
    }

    private static void deployAll(String deployEnv, int threadNum) {
        PublishTask publishTask = new PublishTask(deployEnv, FileConf.fileRootUrl);
        publishTask.publishAsync(threadNum);
    }
}
