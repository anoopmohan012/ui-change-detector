package com.uichecker;

import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0].toLowerCase();

        // Optional version argument
        String runId;

        if (mode.equals("compare")) {
            // compare <runId>
            runId = (args.length >= 2)
                    ? args[1]
                    : LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        } else {
            // baseline/current <urls.txt> <runId>
            runId = (args.length >= 3)
                    ? args[2]
                    : LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        Path rootRunDir = Config.OUTPUT_ROOT.resolve(runId);

        Path baselineDir = rootRunDir.resolve("baseline");
        Path currentDir  = rootRunDir.resolve("current");
        Path diffDir     = rootRunDir.resolve("diff");
        Path reportDir   = rootRunDir.resolve("report");

        /* =========================
           BASELINE / CURRENT MODE
           ========================= */

        if (mode.equals("baseline") || mode.equals("current")) {

            if (args.length < 2) {
                System.err.println("❌ Please provide urls.txt");
                System.exit(1);
            }

            Path urlsFile = Paths.get(args[1]);
            List<String> urls = Files.readAllLines(urlsFile).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .collect(Collectors.toList());

            Path outputDir = mode.equals("baseline") ? baselineDir : currentDir;
            Files.createDirectories(outputDir);

            ScreenshotCapturer.captureList(urls, outputDir);

            System.out.println("✅ Screenshots saved to: " + outputDir.toAbsolutePath());
            return;
        }

        /* =========================
           COMPARE MODE
           ========================= */

        if (mode.equals("compare")) {

            Files.createDirectories(diffDir);
            Files.createDirectories(reportDir);

            List<Path> baselineFiles;
            try (var stream = Files.list(baselineDir)) {
                baselineFiles = stream
                        .filter(p -> p.toString().endsWith(".png"))
                        .collect(Collectors.toList());
            }

            if (baselineFiles.isEmpty()) {
                System.err.println("❌ No baseline images found in " + baselineDir);
                return;
            }

            List<ImageComparator.Result> results = new ArrayList<>();

            for (Path base : baselineFiles) {

                Path current = currentDir.resolve(base.getFileName());

                if (!Files.exists(current)) {
                    System.out.println("⚠️ Skipping (no current image): " + base.getFileName());
                    continue;
                }

                ImageComparator.Result result =
                        ImageComparator.compare(base, current);

                Path diffOut = diffDir.resolve(base.getFileName());
                ImageIO.write(result.diffImage, "PNG", diffOut.toFile());

                System.out.println("🔍 Compared: " + base.getFileName()
                        + " | Change: "
                        + String.format("%.2f", result.changePercent)
                        + "% | Result: "
                        + (result.passed ? "PASS" : "FAIL"));

                results.add(result);
            }

            ReportGenerator.generateHtmlReport(results, reportDir);

            System.out.println("📊 Report available at: "
                    + reportDir.resolve("UI_Change_Report.html").toAbsolutePath());
            return;
        }

        System.err.println("❌ Unknown mode: " + mode);
        printUsage();
        System.exit(2);
    }

    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println("  java -jar ui-change-detector.jar baseline urls.txt [version]");
        System.out.println("  java -jar ui-change-detector.jar current urls.txt [version]");
        System.out.println("  java -jar ui-change-detector.jar compare [version]");
        System.out.println("\nIf version is not provided, timestamp is used.");
    }
}
