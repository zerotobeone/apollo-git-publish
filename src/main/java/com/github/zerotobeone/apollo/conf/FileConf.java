package com.github.zerotobeone.apollo.conf;

import cn.hutool.setting.dialect.Props;

/**
 * FileConf
 *
 * @author zerotobeone
 * @version FileConf.java v1.0 2023-01-05
 */
public class FileConf {
    public static String fileRootUrl;

    static {
        Props props = new Props(System.getProperty("user.home") + "/.apollo-git-publish-cnf");
        fileRootUrl = props.getStr("file.url");
        if (fileRootUrl.trim().startsWith("~")){
            fileRootUrl = System.getProperty("user.home") +  fileRootUrl.substring(1);
        }
        props.clear();
    }
}
