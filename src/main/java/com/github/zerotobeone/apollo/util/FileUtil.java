package com.github.zerotobeone.apollo.util;

import cn.hutool.core.io.file.FileReader;
import com.google.common.base.Strings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileUtil
 *
 * @author zerotobeone
 * @version FileUtil.java v1.0 2023-01-08
 */
public class FileUtil {
    
    public static Map<String, String> readPropertiesFile(String fileUrl){
        Map<String, String> properties = new ConcurrentHashMap<>();
        FileReader fileReader = new FileReader(fileUrl,  "UTF-8");
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
                properties.put(key, value);
            }
        }
        return properties;
    }
}
