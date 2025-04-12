package com.example.sportx.Utils;

import com.example.sportx.Entity.User;
import com.example.sportx.Entity.UserDto;

public class UserHolder {
    private static final ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void saveUser(User user) {
        userHolder.set(user);
    }

    public static User getUser() {
        return userHolder.get();
    }

    public static void removeUser() {
        userHolder.remove();
    }
}
