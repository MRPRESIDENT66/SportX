package com.example.sportx.Utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationKeysTest {

    @Test
    void statusKey_shouldFollowExpectedPattern() {
        String key = NotificationKeys.statusKey(100L, "SIGN_UP_SUCCESS", "u001");

        assertThat(key).isEqualTo("notify:challenge:100:SIGN_UP_SUCCESS:u001");
    }

    @Test
    void scheduledSetKey_shouldReturnFixedKey() {
        assertThat(NotificationKeys.scheduledSetKey()).isEqualTo("notify:challenge:scheduled");
    }
}
