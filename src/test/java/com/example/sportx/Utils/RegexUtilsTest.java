package com.example.sportx.Utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegexUtilsTest {

    @Test
    void isPhone_shouldReturnTrueForValidPhone() {
        // Arrange
        String phone = "13812345678";

        // Act
        boolean result = RegexUtils.isPhone(phone);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isEmailValid() {

        String email1 = "test@example.com";
        String email2 = "user.name+tag@sub.domain.co";
        String email3 = "bad-email@";
        String email4 = "   ";

        assertThat(RegexUtils.isEmail(email1)).isTrue();
        assertThat(RegexUtils.isEmail(email2)).isTrue();
        assertThat(RegexUtils.isEmail(email3)).isFalse();
        assertThat(RegexUtils.isEmail(email4)).isFalse();

    }

    @Test
    void isNumber_shouldReturnTrueForValidNumericStrings() {
        assertThat(RegexUtils.isNumber("123456")).isTrue();
        assertThat(RegexUtils.isNumber("00123")).isTrue();
    }

    @Test
    void isNumber_shouldReturnFalseForInvalidOrBlankInputs() {
        assertThat(RegexUtils.isNumber("12a45")).isFalse();
        assertThat(RegexUtils.isNumber("-123")).isFalse();
        assertThat(RegexUtils.isNumber("12.3")).isFalse();
        assertThat(RegexUtils.isNumber("")).isFalse();
        assertThat(RegexUtils.isNumber("   ")).isFalse();
        assertThat(RegexUtils.isNumber(null)).isFalse();
    }

    @Test
    void isUrl_shouldValidateCommonHttpAndHttpsUrls() {
        assertThat(RegexUtils.isUrl("https://www.example.com")).isTrue();
        assertThat(RegexUtils.isUrl("http://sportx.cn/path?a=1")).isTrue();
        assertThat(RegexUtils.isUrl("not-a-url")).isFalse();
        assertThat(RegexUtils.isUrl("   ")).isFalse();
    }

    @Test
    void isIdCard_shouldValidateFormat() {
        assertThat(RegexUtils.isIdCard("11010119900307123X")).isTrue();
        assertThat(RegexUtils.isIdCard("110101199003071231")).isTrue();
        assertThat(RegexUtils.isIdCard("123456")).isFalse();
        assertThat(RegexUtils.isIdCard(null)).isFalse();
    }
}
