package com.bruburger.tracker;

import org.springframework.security.core.Authentication;

public class SecurityUtils {

    private SecurityUtils() {
        
    }

    public static int getUserId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}