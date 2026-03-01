package com.uichecker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class ImageComparator {

    /* =========================
       RESULT MODELS
       ========================= */

    public static class ChangeDetail {
        public Rectangle area;
        public String changeType;
        public String description;
    }

    public static class Result {
        public String pageName;
        public double changePercent;
        public BufferedImage diffImage;
        public boolean passed;
        public List<Rectangle> boxes = new ArrayList<>();
        public List<ChangeDetail> changes = new ArrayList<>();
    }

    /* =========================
       MAIN COMPARE METHOD
       ========================= */

    public static Result compare(Path baselineImage, Path currentImage) throws IOException {

        BufferedImage base = ImageIO.read(baselineImage.toFile());
        BufferedImage cur = ImageIO.read(currentImage.toFile());

        if (base.getWidth() != cur.getWidth() || base.getHeight() != cur.getHeight()) {
            int w = Math.max(base.getWidth(), cur.getWidth());
            int h = Math.max(base.getHeight(), cur.getHeight());
            base = padImage(base, w, h, Color.WHITE);
            cur = padImage(cur, w, h, Color.WHITE);
        }

        int w = base.getWidth();
        int h = base.getHeight();
        boolean[][] diffMask = new boolean[h][w];
        long changedPixels = 0;

        // Grayscale pixel comparison (less noisy)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int g1 = toGray(base.getRGB(x, y));
                int g2 = toGray(cur.getRGB(x, y));

                if (Math.abs(g1 - g2) > Config.DIFF_PIXEL_THRESHOLD) {
                    diffMask[y][x] = true;
                    changedPixels++;
                }
            }
        }

        double changePercent = (changedPixels / (double) (w * h)) * 100.0;

        List<Rectangle> boxes = extractBoundingBoxes(diffMask);
        boxes = mergeNearbyBoxes(boxes);
       // boxes = consolidateToTextBand(boxes);
        boxes = consolidateByHorizontalBand(boxes);

        List<ChangeDetail> changeDetails =
                detectChangeTypes(base, cur, boxes, changePercent);

        BufferedImage annotated = deepCopy(cur);
        Graphics2D g = annotated.createGraphics();
        g.setStroke(new BasicStroke(3));
        g.setFont(new Font("Arial", Font.BOLD, 14));

        for (ChangeDetail cd : changeDetails) {
            Rectangle r = cd.area;
            g.setColor(new Color(255, 0, 0, 180));
            g.drawRect(r.x, r.y, r.width, r.height);
            g.drawString(cd.changeType, r.x + 5, Math.max(r.y - 5, 15));
        }
        g.dispose();

        Result result = new Result();
        result.pageName = baselineImage.getFileName().toString();
        result.changePercent = changePercent;
        result.diffImage = annotated;
        result.passed = changePercent < Config.PASS_PERCENT_THRESHOLD;
        result.boxes = boxes;
        result.changes = changeDetails;

        return result;
    }

    /* =========================
       CHANGE TYPE DETECTION
       ========================= */

    private static List<ChangeDetail> detectChangeTypes(
            BufferedImage base,
            BufferedImage cur,
            List<Rectangle> boxes,
            double changePercent) {

        List<ChangeDetail> list = new ArrayList<>();

        for (Rectangle r : boxes) {

            ChangeDetail cd = new ChangeDetail();
            cd.area = r;

            // --- Safe crop bounds ---
            int width = Math.min(r.width, base.getWidth() - r.x);
            int height = Math.min(r.height, base.getHeight() - r.y);

            if (width <= 0 || height <= 0) continue;

            BufferedImage baseCrop = base.getSubimage(r.x, r.y, width, height);
            BufferedImage curCrop = cur.getSubimage(r.x, r.y, width, height);

            // --- OCR TEXT CHECK FIRST ---
            String baseText = TextExtractor.extract(baseCrop);
            String curText = TextExtractor.extract(curCrop);

            if (!baseText.isEmpty() && !curText.isEmpty()
                    && !baseText.equals(curText)) {

                cd.changeType = "TEXT_CHANGE";
                cd.description =
                        "Text changed from '" + baseText +
                                "' to '" + curText + "'";

            }

            // --- LAYOUT SHIFT CHECK ---
            else if (isLayoutShift(r, changePercent)) {

                cd.changeType = "LAYOUT_SHIFT";
                cd.description = "Element position or layout changed";

            }

            // --- COLOR CHANGE CHECK ---
            else if (isMostlyColorChange(base, cur, r)) {

                cd.changeType = "COLOR_CHANGE";
                cd.description = "Styling or color modification detected";

            }

            // --- FALLBACK ---
            else {

                cd.changeType = "CONTENT_CHANGE";
                cd.description = "Visual content updated (text/image)";
            }

            list.add(cd);
        }

        return list;
    }

    private static boolean isMostlyColorChange(
            BufferedImage base,
            BufferedImage cur,
            Rectangle r) {

        int samples = 0;
        int diffs = 0;

        for (int y = r.y; y < r.y + r.height; y += 4) {
            for (int x = r.x; x < r.x + r.width; x += 4) {

                if (x < base.getWidth() && y < base.getHeight()) {

                    samples++;
                    int g1 = toGray(base.getRGB(x, y));
                    int g2 = toGray(cur.getRGB(x, y));

                    if (Math.abs(g1 - g2) > Config.DIFF_PIXEL_THRESHOLD) {
                        diffs++;
                    }
                }
            }
        }

        return samples > 0 && ((double) diffs / samples) > 0.7;
    }

    private static boolean isLayoutShift(Rectangle r, double changePercent) {

        return r.width > 200 &&
                r.height > 100 &&
                changePercent > 5.0;
    }

    /* =========================
       IMAGE HELPERS
       ========================= */

    private static int toGray(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (r + g + b) / 3;
    }

    private static BufferedImage padImage(BufferedImage src, int w, int h, Color bg) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage deepCopy(BufferedImage img) {
        BufferedImage copy = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = copy.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return copy;
    }

    /* =========================
       BOUNDING BOX LOGIC
       ========================= */

    private static List<Rectangle> extractBoundingBoxes(boolean[][] mask) {

        int h = mask.length;
        int w = mask[0].length;
        boolean[][] visited = new boolean[h][w];
        List<Rectangle> boxes = new ArrayList<>();

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                if (mask[y][x] && !visited[y][x]) {

                    int minX = x, minY = y, maxX = x, maxY = y;
                    Queue<int[]> q = new ArrayDeque<>();
                    q.add(new int[]{x, y});
                    visited[y][x] = true;

                    while (!q.isEmpty()) {
                        int[] p = q.poll();

                        for (int i = 0; i < 4; i++) {

                            int nx = p[0] + dx[i];
                            int ny = p[1] + dy[i];

                            if (nx >= 0 && ny >= 0 && nx < w && ny < h &&
                                    mask[ny][nx] && !visited[ny][nx]) {

                                visited[ny][nx] = true;
                                q.add(new int[]{nx, ny});

                                minX = Math.min(minX, nx);
                                minY = Math.min(minY, ny);
                                maxX = Math.max(maxX, nx);
                                maxY = Math.max(maxY, ny);
                            }
                        }
                    }

                    int bw = maxX - minX + 1;
                    int bh = maxY - minY + 1;
                    int area = bw * bh;

                    // Increased noise filtering threshold
                    if (area > 350) {
                        boxes.add(new Rectangle(minX, minY, bw, bh));
                    }
                }
            }
        }

        return boxes;
    }

    /* =========================
       MERGE NEARBY BOXES
       ========================= */

    private static List<Rectangle> mergeNearbyBoxes(List<Rectangle> boxes) {

        if (boxes.isEmpty()) return boxes;

        List<Rectangle> merged = new ArrayList<>(boxes);

        boolean changed;

        do {
            changed = false;
            List<Rectangle> newList = new ArrayList<>();
            boolean[] used = new boolean[merged.size()];

            for (int i = 0; i < merged.size(); i++) {

                if (used[i]) continue;

                Rectangle current = new Rectangle(merged.get(i));

                for (int j = i + 1; j < merged.size(); j++) {

                    if (used[j]) continue;

                    Rectangle other = merged.get(j);

                    Rectangle expanded = new Rectangle(current);
                    expanded.grow(50, 30);   // aggressive tolerance

                    if (expanded.intersects(other)) {
                        current = current.union(other);
                        used[j] = true;
                        changed = true;
                    }
                }

                newList.add(current);
            }

            merged = newList;

        } while (changed);  // keep merging until stable

        return keepDominantRegion(removeSmallRelativeBoxes(merged));
    }

    private static List<Rectangle> consolidateByHorizontalBand(List<Rectangle> boxes) {

        if (boxes.isEmpty()) return boxes;

        int bandTolerance = 50;   // text line grouping tolerance

        List<Rectangle> result = new ArrayList<>();

        for (Rectangle box : boxes) {

            boolean merged = false;

            for (int i = 0; i < result.size(); i++) {

                Rectangle existing = result.get(i);

                if (Math.abs(existing.y - box.y) < bandTolerance) {
                    result.set(i, existing.union(box));
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                result.add(new Rectangle(box));
            }
        }

        return result;
    }
    private static List<Rectangle> consolidateToTextBand(List<Rectangle> boxes) {

        if (boxes.isEmpty()) return boxes;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;

        for (Rectangle r : boxes) {
            minX = Math.min(minX, r.x);
            minY = Math.min(minY, r.y);
            maxX = Math.max(maxX, r.x + r.width);
            maxY = Math.max(maxY, r.y + r.height);
        }

        Rectangle consolidated = new Rectangle(
                minX,
                minY,
                maxX - minX,
                maxY - minY
        );

        return Collections.singletonList(consolidated);
    }
    private static List<Rectangle> keepDominantRegion(List<Rectangle> boxes) {

        if (boxes.isEmpty()) return boxes;

        Rectangle largest = boxes.get(0);
        int maxArea = largest.width * largest.height;

        for (Rectangle r : boxes) {
            int area = r.width * r.height;
            if (area > maxArea) {
                largest = r;
                maxArea = area;
            }
        }

        return Collections.singletonList(largest);
    }

    private static List<Rectangle> removeSmallRelativeBoxes(List<Rectangle> boxes) {

        if (boxes.size() <= 1) return boxes;

        int maxArea = 0;

        for (Rectangle r : boxes) {
            maxArea = Math.max(maxArea, r.width * r.height);
        }

        List<Rectangle> filtered = new ArrayList<>();

        for (Rectangle r : boxes) {

            int area = r.width * r.height;

            // Remove boxes smaller than 20% of largest box
            if (area > maxArea * 0.2) {
                filtered.add(r);
            }
        }

        return filtered;
    }

}