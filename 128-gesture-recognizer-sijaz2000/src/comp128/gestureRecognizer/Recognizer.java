package comp128.gestureRecognizer;

import edu.macalester.graphics.Point;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Recognizer to recognize 2D gestures. Uses the $1 gesture recognition algorithm.
 */
public class Recognizer {


    int size;
    int n;
    Point origin;
    Map<Deque<Point>,String> templates;
    String selectedTemplates; 

    /**
     * Constructs a recognizer object
     */
    public Recognizer(){
        size = 250;
        n = 64;
        origin = new Point(0,0);
        templates = new HashMap<>();
    }


    /**
     * Create a template to use for matching
     * @param name of the template
     * @param points in the template gesture's path
     */
    public void addTemplate(String name, Deque<Point> points){
        templates.put(regularizedPoints(points), name);
    }

    /**
     * Compares the gestures with the templates and gives the score of how closely a gesture resembles a template 
     * @param points
     * @return the best score after comparing a newly drawn gesture with an added template 
     */

    public double recognize(Deque<Point> points){
        double score = 0;
        double distance = 0;
        double minDistance = 100000000;
        Deque<Point> regularizedPoints = regularizedPoints(points);

        for (Map.Entry<Deque<Point>,String> template : templates.entrySet()) {
            distance = distanceAtBestAngle(regularizedPoints, template.getKey());
            if(distance <= minDistance){
                minDistance = distance;
                selectedTemplates = template.getValue();
            }      
        }
        score = 1-(minDistance/((Math.sqrt(Math.pow(boundingBox(regularizedPoints).getX(),2)+Math.pow(boundingBox(regularizedPoints).getY(), 2)))/2));

        return score;
    }

    /**
     * @return the template that has the score closed to 1 in the recognize method 
     */
    public String getTemplates(){
        return selectedTemplates;
    }


    /**
     * Uses a golden section search to calculate rotation that minimizes the distance between the gesture and the template points.
     * @param points
     * @param templatePoints
     * @return best distance
     */
    private double distanceAtBestAngle(Deque<Point> points, Deque<Point> templatePoints){
        double thetaA = -Math.toRadians(45);
        double thetaB = Math.toRadians(45);
        final double deltaTheta = Math.toRadians(2);
        double phi = 0.5*(-1.0 + Math.sqrt(5.0));// golden ratio
        double x1 = phi*thetaA + (1-phi)*thetaB;
        double f1 = distanceAtAngle(points, templatePoints, x1);
        double x2 = (1 - phi)*thetaA + phi*thetaB;
        double f2 = distanceAtAngle(points, templatePoints, x2);
        while(Math.abs(thetaB-thetaA) > deltaTheta){
            if (f1 < f2){
                thetaB = x2;
                x2 = x1;
                f2 = f1;
                x1 = phi*thetaA + (1-phi)*thetaB;
                f1 = distanceAtAngle(points, templatePoints, x1);
            }
            else{
                thetaA = x1;
                x1 = x2;
                f1 = f2;
                x2 = (1-phi)*thetaA + phi*thetaB;
                f2 = distanceAtAngle(points, templatePoints, x2);
            }
        }
        return Math.min(f1, f2);
    }

    private double distanceAtAngle(Deque<Point> points, Deque<Point> templatePoints, double theta){
        Deque<Point> rotatedPoints = null;
        rotatedPoints = rotateBy(points, theta);
        return pathDistance(rotatedPoints, templatePoints);
    }

    double pathDistance(Deque<Point> a, Deque<Point> b){
        double distance = 0;
        Iterator<Point> iter1 = a.iterator();
        Iterator<Point> iter2 = b.iterator();

        Point nextA = null;
        Point nextB = null; 

        while (iter1.hasNext()){
            nextA = iter1.next();
            nextB = iter2.next();

            distance += nextA.distance(nextB);
        }
        distance = distance /a.size();
        return distance;
    }


    /**
     * Calculates the distance between two points in the gesture
     * @param points
     * @return the path length
     */
    public double pathLength(Deque<Point> points){
        double length = 0;
        Point previousPoint = null; 
        for (Point point : points) {
            if (previousPoint!= null){
                length += previousPoint.distance(point); 
            }
            previousPoint=point;
        }
        return length;
    }
    
    /**
     * Resamples the original gesture points into 64 points 
     * @param points
     * @param n
     * @return a deque of resampled gesture points 
     */
    public Deque<Point> resample(Deque<Point> points, int n){
        double segmentDistance = 0.0;
        double accumulatedDistance = 0.0;
        double resampleInterval = pathLength(points) /(n-1.0);
        Deque<Point> resampledPoints = new ArrayDeque<>();
        resampledPoints.add(points.getFirst());
        Iterator<Point> iter = points.iterator();
        Point previousPoint = iter.next();
        Point currentPoint = iter.next();
        
        while (iter.hasNext()){
            segmentDistance = previousPoint.distance(currentPoint);
            if ((accumulatedDistance + segmentDistance) >= resampleInterval){
                double alphaValue = (resampleInterval - accumulatedDistance)/segmentDistance;
                Point resamplePoint = Point.interpolate(previousPoint, currentPoint, alphaValue);
                resampledPoints.add(resamplePoint);
                previousPoint = resamplePoint;
                accumulatedDistance = 0.0;
            } 
            else {
                accumulatedDistance += segmentDistance;
                previousPoint = currentPoint;
                currentPoint = iter.next();
            }

        }
        if (resampledPoints.size() < n){
            resampledPoints.add(points.getLast());
        }
        return resampledPoints;

    }
        
    /**
     * Calculates the centroid of the gesture points 
     * @param points
     * @return the centroid Point 
     */

    public Point centroid(Deque<Point> points) {
        double sumX = 0;
        double sumY = 0;
        for (Point point : points) {
            sumX += point.getX();
            sumY += point.getY();
        }

        double avgX = sumX/points.size();
        double avgY = sumY/points.size();
        Point centroid = new Point(avgX,avgY);
        return centroid;
    }
    
    /**
     * Calculates the indicative angle of the gesture 
     * @param points
     * @return the indicative angle 
     */

    public double indicativeAngle(Deque<Point> points){

        double indicativeAngle = centroid(points).subtract(points.getFirst()).angle();
        return indicativeAngle;    
    }
    

    /**
     * Rotates the resampled gesture by theta around its centroid 
     * @param points
     * @param theta
     * @return the rotated deque of gesture points 
     */
    
    public Deque<Point> rotateBy (Deque<Point> points, double theta){
        Point centroid = centroid(points);
        Deque<Point> rotatedPoints = new ArrayDeque<>();

        for (Point point : points) {
            point = point.rotate(theta, centroid);
            rotatedPoints.add(point);
        }

        return rotatedPoints;
    }

    /**
     * Finds the highest X and Y value in order to create the bounding box
     * @param points
     * @return the point of the gesture with the highest X and Y coordinates 
     */

    public Point boundingBox (Deque<Point> points) {

        double maxX = 0;
        double maxY = 0;
        double minX = 100000000;
        double minY = 100000000;

        for (Point point : points) {
            if (point.getX() > maxX){
                maxX = point.getX();
            }
            if (minX > point.getX()){
                minX = point.getX();            
            }
            if (point.getY()> maxY){
                maxY = point.getY();
            }
            if (minY > point.getY()){
                minY = point.getY();
            }       
        }
        Point boundingPoint = new Point((maxX - minX), (maxY - minY));

        return boundingPoint;
    }

    /**
     * Scales the rotated and resampled points by using the bounding box 
     * @param points
     * @param size
     * @return the deque of scaled gesture points
     */

    public Deque<Point> scaleTo(Deque<Point> points, int size){
        double width = boundingBox(points).getX();
        double height = boundingBox(points).getY();

        Deque<Point> scaledPoints = new ArrayDeque<>();

        double scaleX = size/width;
        double scaleY = size/height;

        for (Point point : points) {
            scaledPoints.add(point.scale(scaleX, scaleY));
        }

        return scaledPoints;
    }

    /**
     * Translates the gesture points to point k 
     * @param points
     * @param k
     * @return deque of translated points 
     */
    public Deque<Point> translateTo(Deque<Point> points, Point k){
        Point centroid = centroid(points);
        double translateX = k.getX() - centroid.getX();
        double translateY = k.getY() - centroid.getY();
        Deque<Point> translatedPoints = new ArrayDeque<>();

        for (Point point : points) {
            translatedPoints.add(new Point (point.getX()+translateX, point.getY()+translateY));
        }

        return translatedPoints;

    }

    /**
     * Regularizes the gesture points according to the 4 step gesture recognizer algorithm 
     * @param points
     * @return deque of regularizedPoints 
     */
    public Deque<Point> regularizedPoints (Deque<Point> points){
        Deque<Point> resampledPoints = resample(points, n);
        Deque<Point> rotatedPoints = rotateBy(resampledPoints, -indicativeAngle(resampledPoints));
        Deque<Point> scaledPoints = scaleTo(rotatedPoints, size);
        Deque<Point> translatedPoints = translateTo(scaledPoints, origin);

        return translatedPoints;

    }












}