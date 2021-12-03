package com.dvbug.util;

import java.util.Random;

public class StringUtil {
    private static final String STR ="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static String randomString(int length){
        Random random=new Random();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(STR.charAt(number));
        }
        return sb.toString();
    }
}
