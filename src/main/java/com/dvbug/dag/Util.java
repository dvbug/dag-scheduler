package com.dvbug.dag;

public class Util {
    public static String covering(String s, int len, String c) {
        return covering(s, len, c, true);
    }

    public static String covering(String s, int len, String c, boolean prefix) {
        String origin = (null == s || s.isEmpty()) ? "" : s;
        int oriLen = origin.length();
        if (oriLen < len) {
            int repeatN = len - oriLen;
            if (prefix) return String.format("%s%s", repeat(c, repeatN), origin);
            else return String.format("%s%s", origin, repeat(c, repeatN));
        } else if (oriLen == len) {
            return origin;
        } else return origin.substring(0, len);
    }

    public static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

}
