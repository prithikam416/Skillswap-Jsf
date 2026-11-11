package com.skillswap.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Which email domains count as "campus" addresses. Only students with such an address can register
 * (verified campus accounts). Change it without touching code via the environment variable
 * SKILLSWAP_COLLEGE_DOMAINS or -Dskillswap.college.domains=nmamit.in,nitte.edu.in (comma separated).
 */
public final class CampusConfig {
    private static final String DEFAULT_DOMAINS = "nmamit.in";

    private CampusConfig() {}

    public static List<String> allowedDomains() {
        String raw = System.getProperty("skillswap.college.domains");
        if (raw == null || raw.trim().isEmpty()) raw = System.getenv("SKILLSWAP_COLLEGE_DOMAINS");
        if (raw == null || raw.trim().isEmpty()) raw = DEFAULT_DOMAINS;
        List<String> list = new ArrayList<>();
        for (String d : raw.split(",")) {
            String t = d.trim().toLowerCase();
            if (t.startsWith("@")) t = t.substring(1);
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    public static boolean isCampusEmail(String email) {
        if (email == null) return false;
        String e = email.trim().toLowerCase();
        int at = e.lastIndexOf('@');
        if (at < 1) return false;
        String domain = e.substring(at + 1);
        for (String allowed : allowedDomains()) {
            if (domain.equals(allowed) || domain.endsWith("." + allowed)) return true;
        }
        return false;
    }

    public static String describe() {
        return String.join(", ", allowedDomains());
    }
}
