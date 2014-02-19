package utils.aggregator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import utils.LoggerUtils;

public class ContourAggregator {

    public Polygon[] polygons;
    public int max_x;
    public int max_y;
    public boolean[][] mask;
    public int[] iMask;
    public static double THRESHOLD = 0.4;

    public ContourAggregator(Polygon[] polygons, int max_x, int max_y) {
        this.polygons = polygons;
        this.max_x = max_x;
        this.max_y = max_y;
        this.mask = new boolean[max_x][max_y];
        this.iMask = new int[max_x * max_y];
    }

    public ContourAggregator(String[] polygons, int max_x, int max_y) {
        this.polygons = new Polygon[polygons.length];
        for (int j = 0; j < polygons.length; j++) {
            JSONArray pointsArr = (JSONArray) JSONSerializer.toJSON(polygons[j]);
            int[] xPoints = new int[pointsArr.size()];
            int[] yPoints = new int[pointsArr.size()];
            for (int i = 0; i < pointsArr.size(); i++) {
                JSONObject point = pointsArr.getJSONObject(i);
                xPoints[i] = point.getInt("x");
                yPoints[i] = point.getInt("y");
            }
            this.polygons[j] = new Polygon(xPoints, yPoints, xPoints.length);
        }
        this.max_x = max_x;
        this.max_y = max_y;
        this.mask = new boolean[max_x][max_y];
        this.iMask = new int[max_x * max_y];
    }

    public Image getMask() {
        BufferedImage im = new BufferedImage(max_x, max_y, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster raster = im.getRaster();
        for (int x = 0; x < max_x; x++) {
            for (int y = 0; y < max_y; y++) {
                int voters = 0;
                for (Polygon p : polygons) {
                    if (p.contains(x, y)) {
                        voters++;
                    }
                }
                if (((double) voters / polygons.length) > THRESHOLD) {
                    raster.setSample(x, y, 0, 1);
                } else {
                    raster.setSample(x, y, 0, 0);
                }
                this.iMask[x * max_y + y] = (((double) voters / polygons.length) > THRESHOLD) ? 1 : 0;
            }
        }

        for (int u = 0; u < 30; u++) {
            for (int x = 1; x < max_x - 1; x++) {
                for (int y = 1; y < max_y - 1; y++) {
                    if (raster.getSample(x, y, 0) == 0) {
                        if (surroundedColor(raster, x, y, 1) > 5) {
                            raster.setSample(x, y, 0, 1);
                        }
                    }
                }
            }
        }

        for (int u = 0; u < 30; u++) {
            for (int x = 1; x < max_x - 1; x++) {
                for (int y = 1; y < max_y - 1; y++) {
                    if (raster.getSample(x, y, 0) == 1) {
                        if (surroundedColor(raster, x, y, 0) > 5) {
                            raster.setSample(x, y, 0, 0);
                        }
                    }
                }
            }
        }
        return im;

    }

    public Image getBitmapContour() {
        Image im2 = getMask();
        WritableRaster rst = ((BufferedImage) im2).getRaster();
        java.util.List<Integer> xToDestroy = new ArrayList<>();
        java.util.List<Integer> yToDestroy = new ArrayList<>();

        for (int x = 1; x < max_x - 1; x++) {
            for (int y = 1; y < max_y - 1; y++) {
                if (surroundedWithPoints(rst, x, y)) {
                    xToDestroy.add(x);
                    yToDestroy.add(y);
                }
            }
        }
        for (int i = 1; i < xToDestroy.size(); i++) {
            rst.setSample(xToDestroy.get(i).intValue(), yToDestroy.get(i).intValue(), 0, 0);
        }
        return im2;
    }

    public static boolean moreThan2Around(final WritableRaster raster, int x, int y) {
        int counter = 0;
        if (raster.getSample(x - 1, y - 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x - 1, y, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x - 1, y + 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x, y - 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x, y + 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x + 1, y - 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x + 1, y, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x + 1, y + 1, 0) == 1) {
            counter++;
        }
        if (counter > 1) {
            return true;
        }
        return false;
    }

    public static int surroundedColor(final WritableRaster raster, int x, int y, int color) {
        int counter = 0;
        if (raster.getSample(x - 1, y - 1, 0) == color) {
            counter++;
        }
        if (raster.getSample(x - 1, y, 0) == color) {
            counter++;
        }
        if (raster.getSample(x - 1, y + 1, 0) == color) {
            counter++;
        }
        if (raster.getSample(x, y - 1, 0) == color) {
            counter++;
        }
        if (raster.getSample(x, y + 1, 0) == color) {
            counter++;
        }
        if (raster.getSample(x + 1, y - 1, 0) == color) {
            counter++;
        }
        if (raster.getSample(x + 1, y, 0) == color) {
            counter++;
        }
        if (raster.getSample(x + 1, y + 1, 0) == color) {
            counter++;
        }
        return counter;
    }

    public static boolean lessThan2Around(final WritableRaster raster, int x, int y) {
        int counter = 0;
        if (raster.getSample(x - 1, y - 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x - 1, y, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x - 1, y + 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x, y - 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x, y + 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x + 1, y - 1, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x + 1, y, 0) == 1) {
            counter++;
        }
        if (raster.getSample(x + 1, y + 1, 0) == 1) {
            counter++;
        }
        if (counter < 2) {
            return true;
        }
        return false;
    }

    public static boolean surroundedWithPoints(final WritableRaster raster, int x, int y) {
        if (raster.getSample(x - 1, y - 1, 0) != 1) {
            return false;
        }
        if (raster.getSample(x - 1, y, 0) != 1) {
            return false;
        }
        if (raster.getSample(x - 1, y + 1, 0) != 1) {
            return false;
        }
        if (raster.getSample(x, y - 1, 0) != 1) {
            return false;
        }
        if (raster.getSample(x, y + 1, 0) != 1) {
            return false;
        }
        if (raster.getSample(x + 1, y - 1, 0) != 1) {
            return false;
        }
        if (raster.getSample(x + 1, y, 0) != 1) {
            return false;
        }
        if (raster.getSample(x + 1, y + 1, 0) != 1) {
            return false;
        }
        return true;
    }

    public static Image simpleAggregator(String[] contours, Integer width, Integer height) throws Exception {
        ContourAggregator ca;
        try {
            ca = new ContourAggregator(contours, width, height);
            Image im = ca.getMask();
            return im;
        } catch (Exception e) {
            LoggerUtils.error("CONTOUR", e);
            return null;
        }
    }
}
