package models;

public class Player {

    private int points;
    private int warningsReceived;

    public Player() {
        points = 0;
        warningsReceived = 0;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public int getWarningsReceived() {
        return warningsReceived;
    }

    public int getPoints() {
        return points;
    }

    public void setWarningsReceived(int warningsReceived) {
        this.warningsReceived = warningsReceived;
    }
}
