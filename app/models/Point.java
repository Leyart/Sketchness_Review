package models;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.Json;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class Point {

    private int x;
    private int y;
    private int size;
    private boolean removed;
    private String color;

    public Point(int x, int y, int size, boolean removed, String color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.removed = removed;
        this.color = color;
    }

    public Point() {
    }

    public String getColor() {
        return color;
    }

    public boolean getRemoved() {
        return removed;
    }

    public int getSize() {
        return size;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    //Given the center of a circle and its radius, checks if the points lies
    //inside the circle
    public boolean inRange(int cx, int cy, int size) {
        //Check to see the distance between the point and the center of the circle
        double d = Math.sqrt(Math.pow((x - cx), 2) + Math.pow((y - cy), 2));
        if (d <= (size / 2)) {
            return true;
        } else {
            return false;
        }
    }

    public JsonNode toJson() {
        ObjectNode json = Json.newObject();
        json.put("x", x);
        json.put("y", y);
        json.put("size", size);
        json.put("color", color);
        json.put("removed", removed);
        return json;
    }
}
