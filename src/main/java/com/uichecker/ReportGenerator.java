package com.uichecker;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportGenerator {

    public static void generateHtmlReport(
            List<ImageComparator.Result> results,
            Path reportDir) throws IOException {

        Files.createDirectories(reportDir);

        StringBuilder html = new StringBuilder();

        html.append("<!doctype html>")
                .append("<html><head><meta charset='utf-8'>")
                .append("<title>UI Change Detection Report</title>")
                .append("<style>")
                .append("body{font-family:Arial;padding:20px;}")
                .append(".card{border:1px solid #ddd;padding:15px;margin:20px 0;}")
                .append("img{max-width:30%;height:auto;border:1px solid #ccc;margin-right:10px;}")
                .append(".bad{color:#c00;font-weight:bold;}")
                .append(".good{color:#080;font-weight:bold;}")
                .append(".label{font-weight:bold;margin-top:10px;}")
                .append("table{border-collapse:collapse;margin-top:10px;}")
                .append("th,td{border:1px solid #ccc;padding:6px 10px;}")
                .append("</style>")
                .append("</head><body>");

        html.append("<h1>UI Change Detection Report</h1>");
        html.append("<p>Generated on: ")
                .append(java.time.ZonedDateTime.now())
                .append("</p>");

        for (ImageComparator.Result r : results) {

            String pageName = r.pageName.replace(".png", "");



            html.append("<div class='card'>");
            html.append("<h2>Page: ").append(pageName).append("</h2>");

            html.append("<p>Total Change: ")
                    .append(String.format("%.2f", r.changePercent))
                    .append("% — ")
                    .append(r.passed
                            ? "<span class='good'>PASS</span>"
                            : "<span class='bad'>FAIL</span>")
                    .append("</p>");

            html.append("<div>");
            html.append("<div class='label'>Baseline</div>");
            html.append("<img src='../baseline/")
                    .append(r.pageName)
                    .append("'/>");

            html.append("<div class='label'>Current</div>");
            html.append("<img src='../current/")
                    .append(r.pageName)
                    .append("'/>");

            html.append("<div class='label'>Detected Changes</div>");
            html.append("<img src='../diff/")
                    .append(r.pageName)
                    .append("'/>");
            html.append("</div>");

            if (!r.changes.isEmpty()) {
                html.append("<h3>Detected Change Details</h3>");
                html.append("<table>");
                html.append("<tr>")
                        .append("<th>#</th>")
                        .append("<th>Change Type</th>")
                        .append("<th>Description</th>")
                        .append("<th>Area (x,y,w,h)</th>")
                        .append("</tr>");

                int idx = 1;
                for (ImageComparator.ChangeDetail cd : r.changes) {
                    html.append("<tr>");
                    html.append("<td>").append(idx++).append("</td>");
                    html.append("<td>").append(cd.changeType).append("</td>");
                    html.append("<td>").append(cd.description).append("</td>");
                    html.append("<td>")
                            .append(cd.area.x).append(", ")
                            .append(cd.area.y).append(", ")
                            .append(cd.area.width).append(", ")
                            .append(cd.area.height)
                            .append("</td>");
                    html.append("</tr>");
                }
                html.append("</table>");
            } else {
                html.append("<p>No significant UI changes detected.</p>");
            }

            html.append("</div>");
        }

        html.append("</body></html>");

        // ✅ UNIQUE REPORT PER RUN
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path reportFile = reportDir.resolve("UI_Change_Report_" + timestamp + ".html");
        Files.writeString(reportFile, html.toString());

        System.out.println("✅ Report generated at: " + reportFile.toAbsolutePath());
    }
}
