package com.github.zerotobeone.apollo.conf;

import cn.hutool.setting.dialect.Props;

/**
 * ApolloConf
 *
 * @author  zerotobeone
 * @version ApolloConf.java v1.0 2023-01-05
 */
public class ApolloConf {

    public static String url;

    public static String token;

    public static String releaseBy;

    public static String releaseComment;

    public static String releaseTitle;

    static {
        Props props = new Props(System.getProperty("user.home") + "/.apollo-git-publish-cnf");
        url = props.getStr("apollo.url");
        token = props.getStr("apollo.token");
        releaseBy = props.getStr("apollo.releaseBy", "apollo-git");
        releaseComment = props.getStr("apollo.releaseComment","default comment by apollo-git-publish" );
        releaseTitle = props.getStr("apollo.releaseTitle", "default title by apollo-git-publish");
        props.clear();
    }
}
