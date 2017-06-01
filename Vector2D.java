import java.util.Random;

/**
 * Created by jonahschueller on 03.03.17.
 */
public class Vector2D {

    private double x, y;

    public Vector2D() {
        x = 0;
        y = 0;
    }

    public Vector2D(double x, double y){
        this.x = x;
        this.y = y;
    }

    public void multi(double f){
        x *= f;
        y *= f;
    }


    public void normalize(){
        double len = len();
        if (len != 0) {
            double fac = 1 / len;
            multi(fac);
        }else {
            try {
                throw new Vector2DException("Cannot normalize a zero vector.");
            } catch (Vector2DException e) {
                e.printStackTrace();
            }
        }
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public Vector2D clone() {
        return new Vector2D(x, y);
    }

    public void setMag(double mag){
        double len = len();
        if (len != 0) {
            double f = mag / len;
            multi(f);
        }else {
            try {
                throw new Vector2DException("Value of the vector is zero.");
            } catch (Vector2DException e) {
                e.printStackTrace();
            }
        }
    }

    public void add(Vector2D vec){
        x += vec.getX();
        y += vec.getY();
    }

    public void sub(Vector2D vec){
        x -= vec.getX();
        y -= vec.getY();
    }

    public double len(){
        return Maths.pythagoras1(x, y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }


    public static Vector2D createVector(){
        return new Vector2D();
    }

    public static Vector2D createVector(double x, double y){
        return new Vector2D(x, y);
    }

    public static Vector2D createNormalizedVector(double x, double y){
        Vector2D vec = createVector(x, y);
        vec.normalize();
        return vec;
    }

    public static Vector2D randomVector(){
        Random random = new Random();
        double x = random.nextFloat();
        double y = random.nextFloat();
        Vector2D vec = new Vector2D(x, y);
        return vec;
    }

    public static Vector2D randomVector(double bounds){
        Vector2D vec = randomVector();
        vec.multi(bounds);
        return vec;
    }

    @Override
    public boolean equals(Object vec) {
        if (!(vec instanceof Vector2D)){
            return false;
        }
        Vector2D v = (Vector2D) vec;
        return (v.getX() == getX()) && (v.getY() == getY());
    }

    @Override
    public String toString() {
        return "X: " + getX() + ", Y: " + getY();
    }



    private class Vector2DException extends Exception{
        public Vector2DException(String message) {
            super(message);
        }
    }
}

