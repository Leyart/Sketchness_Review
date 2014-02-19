package models;

import java.util.ArrayList;
import java.util.ListIterator;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

public class Segment {

    private ArrayList<ArrayList<Point>> points;
    private String eraser;

    public Segment(String eraser) {
        points = new ArrayList<>();
        this.eraser = eraser;
    }

    public void ensureCapacity(int row, int column) {
        while (row >= getRowSize()) {
            points.add(new ArrayList<Point>());
        }
        while (column >= getColumnSize(row)) {
            points.get(row).add(new Point());
        }
    }

    public void setPoint(int row, int column, Point point) {
        ensureCapacity(row, column);
        points.get(row).set(column, point);
    }

    public Point getPoint(int row, int column) {
        return points.get(row).get(column);
    }

    public int getRowSize() {
        return points.size();
    }

    public int getColumnSize(int row) {
        if (getRowSize() > 0) {
            return points.get(row).size();
        } else {
            return 0;
        }
    }

    //Reconstruct the final segment, removing the traces that have been erased and fitting them into the canvas size
    public ArrayNode filter(int taskWidth, int taskHeight, int canvasWidth, int canvasHeight) {

        ArrayNode toReturn = new ArrayNode(JsonNodeFactory.instance);
        ArrayList<PositionedPoint> finalSegment = new ArrayList<>();
        ArrayList<PositionedPoint> copy = new ArrayList<>();
/*
        int fixedWidth = taskWidth;
        int fixedHeight = taskHeight;
        //Just the width is bigger than our canvas
        if (taskWidth > canvasWidth && taskHeight <= canvasHeight) {
            fixedHeight = canvasWidth * taskHeight / taskWidth;
            fixedWidth = fixedHeight * taskWidth / taskHeight;
        } //Just the height is bigger than our canvas
        else if (taskHeight > canvasHeight && taskWidth <= canvasWidth) {
            fixedWidth = canvasHeight * taskWidth / taskHeight;
            fixedHeight = fixedWidth * taskHeight / taskWidth;
        } //Both the width and the height are bigger than our canvas
        else if (taskHeight > canvasHeight && taskWidth > canvasWidth) {
            if (taskHeight > taskWidth) {
                fixedWidth = canvasHeight * taskWidth / taskHeight;
                fixedHeight = fixedWidth * taskHeight / taskWidth;
            } else if (taskHeight < taskWidth){
                fixedHeight = canvasWidth * taskHeight / taskWidth;
                fixedWidth = fixedHeight * taskWidth / taskHeight;
            } else {
                fixedHeight = canvasHeight;
                fixedWidth = canvasWidth * fixedHeight / canvasWidth;
            }
        }
        */

        /*
        The logic for image resizing is the following:
        if there is only one size bigger than our canvas i will generate the ratio based on that size, the other one will fit for sure
        if both sizes are smaller than our canvas I chose the bigger one, the other will fit for sure
        if both sizes are equals to our canvas the ratio will be 1
        if both sizes are bigger than our canvas I chose the bigger one, I generate the ratio based on that and i check if the other size fits,
            if it does i keep that ratio, if it doesn't I use the last size I found in order to generate another ratio, now i'm sure that bot sizes will fit
            and the final ratio will be the first ratio multiplied by the second one.
        Once I have the ratio i need to calculate an offset (because the image is centered in the canvas)
        the offset is the (canvas size - the image size)/2
        this work for every scenario.

        In order to find the right position of the point that i draw i have to pick the current position (which is based on the scale),
            subtract the offset and multiply it by the ratio.



         */

        float ratio = 1;
        float tmpRatio;
        int fixedWidth;
        int fixedHeight;

        if(taskWidth > canvasWidth && taskHeight <= canvasHeight){
            ratio = (float) taskWidth/canvasWidth;
        }
        else if(taskHeight > canvasHeight && taskWidth <= canvasWidth){
            ratio = (float) taskHeight/canvasHeight;
        }
        else if(taskHeight < canvasHeight && taskWidth < canvasWidth){
            if(taskHeight > taskWidth){
                ratio = (float) taskHeight/canvasHeight;
            }
            else{
                ratio = (float) taskWidth/canvasWidth;
            }
        }
        else if(taskHeight > canvasHeight && taskWidth > canvasWidth){
            if(taskHeight > taskWidth){
                tmpRatio = (float) taskHeight/canvasHeight;
                if(canvasWidth > taskWidth/tmpRatio){
                    ratio = tmpRatio;
                }
                else{
                    ratio = tmpRatio * ((taskWidth/tmpRatio)/canvasWidth);
                }
            }
            else if(taskWidth > taskHeight){
                tmpRatio = (float) taskWidth/canvasWidth;
                if(canvasHeight > taskHeight/tmpRatio){
                    ratio = tmpRatio;
                }
                else{
                    ratio = tmpRatio * ((taskHeight/tmpRatio)/canvasHeight);
                }
            }
            else if(taskHeight == taskWidth){
                if(taskHeight - canvasHeight > taskWidth - canvasWidth){
                    ratio = (float) taskHeight/canvasHeight;
                }
                else if(taskHeight - canvasHeight < taskWidth - canvasWidth){
                    ratio = (float) taskWidth/canvasWidth;
                }
            }
        }
        else if(taskHeight == canvasHeight && taskWidth == canvasWidth){
            ratio = 1;
        }


        fixedWidth = Math.round((canvasWidth - taskWidth/ratio)/2);
        fixedHeight = Math.round((canvasHeight - taskHeight/ratio)/2);

        //Make a copy of the traces storing the orders in which the points have been saved
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < points.get(i).size(); j++) {
                finalSegment.add(new PositionedPoint(i, points.get(i).get(j)));
                copy.add(new PositionedPoint(i, points.get(i).get(j)));
            }
        }

        //Remove the points that have been erased, keeping the ones that have 
        //not
        @SuppressWarnings("rawtypes")
        ListIterator it = finalSegment.listIterator(finalSegment.size());
        while (it.hasPrevious()) {
            PositionedPoint current = (PositionedPoint) it.previous();
            Point currentPoint = current.getPoint();
            if (currentPoint.getColor().equals(eraser)) {
                @SuppressWarnings("rawtypes")
                ListIterator copyIt = copy.listIterator(copy.size());
                while (copyIt.hasPrevious()) {
                    PositionedPoint check = (PositionedPoint) copyIt.previous();
                    Point checkPoint = check.getPoint();
                    if (checkPoint.inRange(currentPoint.getX(), currentPoint.getY(), currentPoint.getSize()) && !checkPoint.getColor().equals(eraser) && check.getPosition() <= current.getPosition()) {
                        checkPoint.setRemoved(true);
                    }
                }
            }
        }

        //Return just the ordered list of points to be traced, without the eraser
        //information
        @SuppressWarnings("rawtypes")
        ListIterator copyIt = copy.listIterator();
        while (copyIt.hasNext()) {
            Point current = ((PositionedPoint) copyIt.next()).getPoint();
//            current.setX((taskWidth * (current.getX() - ((canvasWidth - fixedWidth) / 2))) / fixedWidth);
  //          current.setY((taskHeight * (current.getY() - ((canvasHeight - fixedHeight) / 2))) / fixedHeight);
            current.setX(Math.round((current.getX()-fixedWidth)*ratio));
            current.setY(Math.round((current.getY()-fixedHeight)*ratio));
            if (!current.getColor().equals(eraser) && current.getX() >= 0 && current.getY() >= 0) {
                toReturn.add(current.toJson());
            }
        }
        return toReturn;
    }
}

class PositionedPoint {

    int position;
    Point point;

    public PositionedPoint(int position, Point point) {
        this.position = position;
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }

    public int getPosition() {
        return position;
    }
}