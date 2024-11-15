package com.vulnmonitor.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Constants class to hold application-wide constants.
 */
public class Constants {

    // Default Operating Systems List
    public static final List<String> DEFAULT_OS_LIST = Arrays.asList(
        "ALL", "Windows", "Linux", "macOS", "Ubuntu", "Debian",
        "RedHat", "CentOS", "Android", "iOS", "FreeBSD", "Solaris"
    );

    // Default Products List
    public static final List<String> DEFAULT_PRODUCT_LIST = Arrays.asList(
        "ALL", "Chrome", "Firefox", "Internet Explorer", "Edge", "Safari",
        "Adobe Reader", "Adobe Flash Player", "Java", "OpenSSL", "Apache", "Nginx",
        "MySQL", "PostgreSQL", "MongoDB", "Microsoft Office", "Windows Server"
    );

    // Maximum allowed archive limit
    public static final int MAX_ARCHIVED_CVES_ALLOWED = 1000;
}