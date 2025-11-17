package db.security;

import db.entities.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter(value = AccessLevel.NONE)
public class SecurityContext {
    @Getter
    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean hasRole(String roleName) {
        if (!isAuthenticated()) return false;
        return currentUser.getRole().getName().equalsIgnoreCase(roleName);
    }

    public static boolean hasAnyRole(String... roleNames) {
        if (!isAuthenticated()) return false;
        String userRole = currentUser.getRole().getName();
        for (String role : roleNames) {
            if (userRole.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}
