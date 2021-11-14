package comp128.gestureRecognizer;

import edu.macalester.graphics.*;
import edu.macalester.graphics.ui.Button;
import edu.macalester.graphics.ui.TextField;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class GestureApp {

    private CanvasWindow canvas;
    private Recognizer recognizer;
    private IOManager ioManager;
    private GraphicsGroup uiGroup;
    private Button addTemplateButton;
    private TextField templateNameField;
    private GraphicsText matchLabel;
    private GraphicsText shapeName;
    private GraphicsText matchScore;
    private Deque<Point> path;


    public GestureApp(){
        canvas = new CanvasWindow("Gesture Recognizer", 600, 600);
        recognizer = new Recognizer();
        path = new ArrayDeque<>();
        ioManager = new IOManager();
        setupUI();
    }

    /**
     * Create the user interface
     */
    private void setupUI(){
        matchLabel = new GraphicsText("Match: ");
        matchLabel.setFont(FontStyle.PLAIN, 24);
        canvas.add(matchLabel, 10, 30);

        




        uiGroup = new GraphicsGroup();

        templateNameField = new TextField();

        addTemplateButton = new Button("Add Template");
        addTemplateButton.onClick( () -> addTemplate() );

        Point center = canvas.getCenter();
        double fieldWidthWithMargin = templateNameField.getSize().getX() + 5;
        double totalWidth = fieldWidthWithMargin + addTemplateButton.getSize().getX();


        uiGroup.add(templateNameField, center.getX() - totalWidth/2.0, 0);
        uiGroup.add(addTemplateButton, templateNameField.getPosition().getX() + fieldWidthWithMargin, 0);
        canvas.add(uiGroup, 0, canvas.getHeight() - uiGroup.getHeight());

        Consumer<Character> handleKeyCommand = ch -> keyTyped(ch);
        canvas.onCharacterTyped(handleKeyCommand);

        canvas.onMouseDown(event -> {
            removeAllNonUIGraphicsObjects();
            path.clear();
            Point startingPoint = event.getPosition();
            path.add(startingPoint);
        });
        canvas.onDrag(event -> {
            Point gesturePoint = event.getPosition();
            path.add(gesturePoint);
        });

        drawLines(path);

        canvas.onMouseUp(event -> {
            String scoreString = Double.toString(recognizer.recognize(path));
            matchScore = new GraphicsText(scoreString);
            matchScore.setFont(FontStyle.PLAIN,20);
            canvas.add(matchScore,150,30);
            shapeName = new GraphicsText(recognizer.getTemplates());
            shapeName.setFont(FontStyle.PLAIN,20);
            canvas.add(shapeName, 85, 30);
            
        });    

    }

    /**
     * Creates a deque of lines from points added to the path deque
     * adds the line segments onto the canvas 
     */
    public void drawLines(Deque<Point> linePoints) {
        Deque <GraphicsObject> lines = new ArrayDeque<GraphicsObject>();
        canvas.onDrag(event ->{
            GraphicsObject lineSegment = new Line(event.getPreviousPosition().getX() ,event.getPreviousPosition().getY(), linePoints.peekLast().getX(), linePoints.peekLast().getY());
            lines.add(lineSegment);
            canvas.add(lineSegment);
        });

        
    }

    /**
     * Clears the canvas, but preserves all the UI objects
     */
    private void removeAllNonUIGraphicsObjects() {
        canvas.removeAll();
        canvas.add(matchLabel);
        canvas.add(uiGroup);
    }

    /**
     * Handle what happens when the add template button is pressed. This method adds the points stored in path as a template
     * with the name from the templateNameField textbox. If no text has been entered then the template is named with "no name gesture"
     */
    private void addTemplate() {
        String name = templateNameField.getText();
        if (name.isEmpty()){
            name = "no name gesture";
        }
        recognizer.addTemplate(name, path); // Add the points stored in the path as a template

    }

    /**
     * Handles keyboard commands used to save and load gestures for debugging and to write tests.
     * Note, once you type in the templateNameField, you need to call canvas.requestFocus() in order to get
     * keyboard events. This is best done in the mouseDown callback on the canvas.
     */
    public void keyTyped(Character ch) {
        if (ch.equals('L')){
            String name = templateNameField.getText();
            if (name.isEmpty()){
                name = "gesture";
            }
            Deque<Point> points = ioManager.loadGesture(name+".xml");
            if (points != null){
                recognizer.addTemplate(name, points);
                System.out.println("Loaded "+name);
            }
        }
        else if (ch.equals('s')){
            String name = templateNameField.getText();
            if (name.isEmpty()){
                name = "gesture";
            }
            ioManager.saveGesture(path, name, name+".xml");
            System.out.println("Saved "+name);
        }
    }


    public static void main(String[] args){
        GestureApp window = new GestureApp();
    }
}
