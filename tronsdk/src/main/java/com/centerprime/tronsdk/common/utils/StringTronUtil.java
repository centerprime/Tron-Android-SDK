package com.centerprime.tronsdk.common.utils;

import com.centerprime.tronsdk.common.crypto.Hash;
import com.centerprime.tronsdk.common.crypto.Sha256Hash;
import com.centerprime.tronsdk.common.crypto.SymmEncoder;
import com.centerprime.tronsdk.core.config.Parameter;

import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理字符串工具类
 */
public class StringTronUtil {
    /**
     * 判断是否为空
     *
     * @param text
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        if (text == null || "".equals(text.trim()) || text.trim().length() == 0
                || "null".equals(text.trim())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断字符串数组texts中是否有一个字符串为空
     *
     * @param texts
     * @return 如果字符串数组texts中有一个为空或texts为空，返回true;otherwise return false;
     */
    public static boolean isEmpty(String... texts) {
        if (texts == null || texts.length == 0) {
            return true;
        }
        for (String text : texts) {
            if (text == null || "".equals(text.trim()) || text.trim().length() == 0
                    || "null".equals(text.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPasswordValid(String password) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(password)) {
            return false;
        }
        if (password.length() < 6) {
            return false;
        }
        if (password.contains("\\s")) {
            return false;
        }
        //Other rule;
        return true;
    }

    public static boolean isAddressValid(byte[] address) {
        if (address == null || address.length == 0) {
            return false;
        }
        if (address.length != Parameter.CommonConstant.ADDRESS_SIZE) {
            return false;
        }
        byte preFixbyte = address[0];
        if (preFixbyte != Parameter.CommonConstant.ADD_PRE_FIX_BYTE) {
            return false;
        }

        return true;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Hash.hash(input);
        byte[] hash1 = Sha256Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    public static byte[] decode58Check(String input) {
        try {
            byte[] decodeCheck = Base58.decode(input);
            if (decodeCheck.length <= 4) {
                return null;
            }
            byte[] decodeData = new byte[decodeCheck.length - 4];
            System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
            byte[] hash0 = Hash.sha256(decodeData);
            byte[] hash1 = Hash.sha256(hash0);
            if (hash1[0] == decodeCheck[decodeData.length] &&
                    hash1[1] == decodeCheck[decodeData.length + 1] &&
                    hash1[2] == decodeCheck[decodeData.length + 2] &&
                    hash1[3] == decodeCheck[decodeData.length + 3]) {
                return decodeData;
            }
            return null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(addressBase58)) {
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!isAddressValid(address)) {
            return null;
        }
        return address;
    }

    public static boolean isPrivateKeyValid(String priKey) {
        if (isEmpty(priKey)) {
            return false;
        }
        if (priKey.length() != 64) {
            return false;
        }

        return true;
    }

    public static double bigDecimal2(double number) {
        BigDecimal b = new BigDecimal(number);
        return b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static String zeros(int n) {
        return repeat('0', n);
    }

    public static String repeat(char value, int n) {
        return new String(new char[n]).replace("\0", String.valueOf(value));
    }
}
