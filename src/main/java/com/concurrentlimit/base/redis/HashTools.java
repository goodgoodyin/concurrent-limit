package com.concurrentlimit.base.redis;

import java.security.MessageDigest;

class HashTools {

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 得到 sha1
     * @param string 待hash的字符串
     * @author YellowTail
     * @since 2020-05-04
     */
    static String sha1(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(string.getBytes("UTF-8"));

            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;

        StringBuilder buf = new StringBuilder(len * 2);

        // 把密文转换成十六进制的字符串形式
        for (byte b : bytes) {
            buf.append(HEX_DIGITS[(b >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[b & 0x0f]);
        }

        return buf.toString();
    }

}
