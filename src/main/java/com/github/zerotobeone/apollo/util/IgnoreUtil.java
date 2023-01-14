package com.github.zerotobeone.apollo.util;

import java.util.Arrays;

/**
 * IgnoreUtil
 *
 * @author zerotobeone
 * @version IgnoreUtil.java v1.0 2023-01-05
 */
public class IgnoreUtil {
    public static boolean isIgnored(String s) {
        return Arrays.asList("settings.gradle","gradlew.bat","build",".gradle","build.gradle","gradlew","gradle",".DS_Store", ".git", ".idea", ".fleet", "README.md", "src", ".gitignore", "target", "pom.xml").contains(s);
    }
}
