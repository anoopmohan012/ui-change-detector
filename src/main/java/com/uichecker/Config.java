package com.uichecker;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    // Root folder for all executions
    public static final Path OUTPUT_ROOT =
            Paths.get("ui-check-output");

    // Thresholds
    public static final int DIFF_PIXEL_THRESHOLD = 25; // color sensitivity
    public static final double PASS_PERCENT_THRESHOLD = 5.0; // <5% = PASS
}
