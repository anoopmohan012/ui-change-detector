package com.uichecker;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScreenshotCapturer {

    public static void captureList(List<String> urls, Path outputDir) throws Exception {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Visible browser (NON-headless)
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);

        try {
            Files.createDirectories(outputDir);

            AShot ashot = new AShot()
                    .shootingStrategy(ShootingStrategies.viewportPasting(100));

            int index = 1;

            for (String url : urls) {

                System.out.println("📸 Capturing: " + url);
                driver.get(url);
                Thread.sleep(2000); // allow page to load

                Screenshot screenshot = ashot.takeScreenshot(driver);
                BufferedImage image = screenshot.getImage();

                String pageKey = getPageKey(driver.getTitle(), url);
                String fileName = String.format("%02d_%s.png", index, pageKey);

                Path out = outputDir.resolve(fileName);
                ImageIO.write(image, "PNG", out.toFile());

                System.out.println("✅ Saved: " + out.toAbsolutePath());

                index++;
            }

        } finally {
            driver.quit();
        }
    }

    /* =========================
       HELPERS
       ========================= */

    private static String getPageKey(String title, String url) {

        String key = sanitize(title);

        if (key.isEmpty() || key.equals("page")) {
            key = sanitize(url);
        }

        if (key.length() > 40) {
            key = key.substring(0, 40);
        }

        return key;
    }

    private static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "page";
        }

        String safe = input.toLowerCase()
                .replaceAll("https?://", "")
                .replaceAll("www\\.", "")
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");

        return safe.isEmpty() ? "page" : safe;
    }
}
