/**
 * this Position class has x as longitude, and y as latitude
 * @author yinchenli
 */
public class Position {
    //longitude
    private double x;
    //latitude
    private double y;
    
    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public double getX() {
        return x;
    }
    public void setX(double x) {
        this.x = x;
    }
    public double getY() {
        return y;
    }
    public void setY(double y) {
        this.y = y;
    }

}
