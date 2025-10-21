package org.app;

import java.util.HashMap;

/**
 * Класс для проверки принадлежности точки заданной области значений функции
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
     * Метод для валидации значений
     * @return результат
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

    /**
     * Метод для проверки на попадание 
     * @return результат
     */
    public boolean isHit() {
        if (x > 0 && y > 0) return false;
        return quarter2() || quarter3() || quarter4();
    }


    /**
     * Метод для области 2-ой четверти
     * @return результат
     */
    private boolean quarter2() {
        if (x <= 0 && y >= 0) {
            double lhs = (double)x * x + y * y;
            double rhs = (double) r * r;
            return lhs <= rhs + 1e-9;
        }
        return false;
    }

    
    /**
     * Метод для области 3-ей четверти
     * @return результат
     */
    private boolean quarter3() {
        if (x <= 0 && y <= 0) {
            double line = -x / 2.0 - r / 2.0;
            return y >= line - 1e-9;
        }
        return false;
    }


    /**
     * Метод для области 4-ой четверти
     * @return результат
     */
    private boolean quarter4() {
        if (x >= 0 && y <= 0) {
            return x <= r + 1e-9 && y >= -r / 2.0 - 1e-9;
        }
        return false;
    }
    
    //геттеры

    public int getX() { return x; }
    public double getY() { return y; }
    public int getR() { return r; }
}
