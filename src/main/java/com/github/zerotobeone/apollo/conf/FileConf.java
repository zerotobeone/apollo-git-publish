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
        Props props = new Props("apollo.properties");
        fileRootUrl = props.getStr("file.url", FileConf.class.getClassLoader().getResource("").getPath()
                .replace("/target/classes", ""));
        props.clear();
    }
}
