package org.app;

import java.util.HashMap;

/**
 * Проверяет параметры x, y, r: валидация и попадание в область.
 *
 * Описание области:
 * 1 четверть (x>0,y>0) — пусто (false)
 * 2 четверть (x<=0,y>=0) — внутри круга радиуса R, центр (0,0)
 * 3 четверть (x<=0,y<=0) — y >= -x/2 - R/2  (точки выше/на прямой)
 * 4 четверть (x>=0,y<=0) — прямоугольник 0<=x<=R и -R/2<=y<=0
 */
public class PointChecker {
    private final HashMap<String, String> data;
    private int x;
    private double y;
    private int r;

    public PointChecker(HashMap<String, String> data) {
        this.data = data;
    }

    /**
     * Валидация типов и диапазонов:
     * x: integer in [-5, 3]
     * y: float/double in [-3, 3]
     * r: integer in [1, 5]
     */
    public boolean validate() {
        try {
            if (!data.containsKey("x") || !data.containsKey("y") || !data.containsKey("r")) {
                return false;
            }
            x = Integer.parseInt(data.get("x"));
            y = Double.parseDouble(data.get("y").replace(',', '.'));
            r = Integer.parseInt(data.get("r"));

            if (x < -5 || x > 3) return false;
            if (y < -3.0 || y > 3.0) return false;
            if (r < 1 || r > 5) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isHit() {
        // 1st quadrant (x>0 && y>0) => false
        if (x > 0 && y > 0) return false;
        return quarter2() || quarter3() || quarter4();
    }

    private boolean quarter2() {
        // x <= 0, y >= 0, inside circle radius r centered at 0,0
        if (x <= 0 && y >= 0) {
            double lhs = (double)x * x + y * y;
            double rhs = (double) r * r;
            return lhs <= rhs + 1e-9;
        }
        return false;
    }

    private boolean quarter3() {
        // x <= 0, y <= 0, above (или на) линию y = -x/2 - r/2
        if (x <= 0 && y <= 0) {
            double line = -x / 2.0 - r / 2.0;
            // "above" означает y >= line
            return y >= line - 1e-9;
        }
        return false;
    }

    private boolean quarter4() {
        // x >= 0, y <= 0, inside rectangle 0<=x<=r, -r/2<=y<=0
        if (x >= 0 && y <= 0) {
            return x <= r + 1e-9 && y >= -r / 2.0 - 1e-9;
        }
        return false;
    }

    public int getX() { return x; }
    public double getY() { return y; }
    public int getR() { return r; }
}
