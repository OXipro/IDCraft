package com.oxipro.idcraft.core.logging;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import org.slf4j.Logger;

import java.util.List;

public final class StartSummary {

    private static final String DIVIDER = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final String FALLBACK_VERSION = "1.0.0-beta";

    private StartSummary() {
    }

    public static boolean enabled(IConfigFile config) {
        if (config == null) {
            return true;
        }
        try {
            return config.getBoolean(CommonMainConfigPaths.LOGGER_SUMMARY);
        } catch (Exception e) {
            return true;
        }
    }

    public static String className(Object impl) {
        return impl == null ? "N/A" : impl.getClass().getName();
    }

    public static String packageVersion(Class<?> type) {
        if (type == null) {
            return "unknown";
        }
        Package pkg = type.getPackage();
        if (pkg == null) {
            return "unknown";
        }
        String version = pkg.getImplementationVersion();
        if (version == null || version.isBlank()) {
            return "unknown";
        }
        return version;
    }

    public static String idcraftVersion() {
        String version = packageVersion(StartSummary.class);
        return "unknown".equals(version) ? FALLBACK_VERSION : version;
    }

    public static void log(Logger logger, String title, List<String> lines) {
        logger.info(DIVIDER);
        logger.info(title);
        logger.info(" ");
        if (lines != null) {
            for (String line : lines) {
                if (line == null || line.isEmpty()) {
                    logger.info(" ");
                } else {
                    logger.info(line);
                }
            }
        }
        logger.info(DIVIDER);
    }
}
