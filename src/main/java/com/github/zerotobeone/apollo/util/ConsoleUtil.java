package com.github.zerotobeone.apollo.util;

import cn.hutool.core.lang.Console;
import com.google.common.base.Strings;

/**
 * ConsoleUtil
 *
 * @author zerotobeone
 * @version ConsoleUtil.java v1.0 2023-01-05
 */
public class ConsoleUtil {

    /**
     * print the application title (apollogitpubilsh)
     */
    public static void logApplicationTitle(){
        Console.log("   ___                 __          _ __            __   ___     __     \n"
                + "  / _ |___  ___  ___  / /__  ___ _(_) /____  __ __/ /  / (_)__ / /     \n"
                + " / __ / _ \\/ _ \\/ _ \\/ / _ \\/ _ `/ / __/ _ \\/ // / _ \\/ / (_-</ _ \\    \n"
                + "/_/ |_\\___/\\___/ .__/_/\\___/\\_, /_/\\__/ .__/\\_,_/_.__/_/_/___/_//_/    \n"
                + "              /_/          /___/     /_/                               \n"
                + "-- author: zerotobeone -------------------------------------------\n");
    }


    /**
     * get the input from console
     *
     * @param hint hint text
     * @param defaultValue default value if input is empty
     */
    public static String getInput(String hint, String defaultValue){
        Console.log(hint);
        String input = Console.scanner().nextLine().trim();
        if(Strings.isNullOrEmpty(input)){
            if(!Strings.isNullOrEmpty(defaultValue)){
                Console.log(defaultValue);
                return defaultValue;
            }else {
                return "";
            }
        }
        return input;
    }

    /**
     * get the input from console
     *
     * @param hint hint text
     */
    public static String getInput(String hint){
        return getInput(hint, "");
    }
}
