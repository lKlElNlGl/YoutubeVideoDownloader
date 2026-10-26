package com.lklengl.youtubevideodownloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionSupport {
    private static final Pattern SEMVER = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)$");
    private VersionSupport() {}
    public static int compare(String one, String two) {
        int[] a = parse(one);
        int[] b = parse(two);
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        return 0;
    }
    private static int[] parse(String value) {
        Matcher m = SEMVER.matcher(value == null ? "" : value);
        if (!m.matches()) throw new IllegalArgumentException("Неверная версия: " + value);
        return new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
    }
}
