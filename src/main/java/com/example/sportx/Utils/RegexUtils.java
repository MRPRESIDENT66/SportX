package com.example.sportx.Utils;

import java.util.regex.Pattern;

/**
 * 正则表达式工具类，用于校验常见输入格式
 */
public class RegexUtils {

    // 手机号正则表达式（中国大陆11位手机号）
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    // 邮箱正则表达式（常见邮箱格式）
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    // 身份证号正则表达式（中国大陆18位身份证号）
    private static final String ID_CARD_REGEX = "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$";
    // URL正则表达式（常见 HTTP/HTTPS 地址）
    private static final String URL_REGEX = "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$";
    // 纯数字正则表达式
    private static final String NUMBER_REGEX = "^\\d+$";

    /**
     * 校验手机号（中国大陆11位手机号）
     * - 以 1 开头，第二位为 3-9，后面跟 9 位数字
     * @param phone 手机号字符串
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(PHONE_REGEX, phone);
    }

    /**
     * 校验邮箱
     * - 支持字母、数字、常见特殊字符，格式为 username@domain.tld
     * @param email 邮箱字符串
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(EMAIL_REGEX, email);
    }

    /**
     * 校验身份证号（中国大陆18位）
     * - 格式：6位地区码 + 8位出生日期 + 3位顺序码 + 1位校验码（0-9或X）
     * @param idCard 身份证号字符串
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(ID_CARD_REGEX, idCard);
    }

    /**
     * 校验 URL 地址
     * - 支持 http:// 或 https:// 开头，域名和路径可选
     * @param url URL 字符串
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(URL_REGEX, url);
    }

    /**
     * 校验是否为纯数字
     * - 只包含 0-9 的字符
     * @param number 数字字符串
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(NUMBER_REGEX, number);
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试手机号
        System.out.println("手机号 13812345678: " + isPhone("13812345678")); // true
        System.out.println("手机号 12345678901: " + isPhone("12345678901")); // false

        // 测试邮箱
        System.out.println("邮箱 test@example.com: " + isEmail("test@example.com")); // true
        System.out.println("邮箱 test@.com: " + isEmail("test@.com")); // false

        // 测试身份证号
        System.out.println("身份证 11010119900307123X: " + isIdCard("11010119900307123X")); // true
        System.out.println("身份证 123456: " + isIdCard("123456")); // false

        // 测试 URL
        System.out.println("URL https://www.example.com: " + isUrl("https://www.example.com")); // true
        System.out.println("URL example: " + isUrl("example")); // false

        // 测试纯数字
        System.out.println("数字 123456: " + isNumber("123456")); // true
        System.out.println("数字 123abc: " + isNumber("123abc")); // false
    }
}
