package net.bluecow.robot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Playfield
 */
public class Playfield extends JPanel {
    private Square[][] squares;
    private int squareWidth = 30;
    private ImageIcon goalIcon;
    
    /**
     * Creates a playfield of spaces (mainly for testing).
     * 
     * @param x Width (in squares)
     * @param y Height (in squares)
     */
    public Playfield(int x, int y) {
        squares = new Square[x][y];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                squares[i][j] = new Square(Square.EMPTY);
            }
        }
    }
    
    /**
     * Creates a new playfield with the specified map.
     * 
     * @param map The map.
     */
    public Playfield(Square[][] map) {
       this.squares = map; 
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < squares.length; i++) {
            for (int j = 0; j < squares[0].length; j++) {
                Rectangle r = new Rectangle(i*squareWidth, j*squareWidth, squareWidth, squareWidth);
                if (squares[i][j].getType() == Square.EMPTY) {
                    Color c = Color.white;
                    g2.setColor(c);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(c.darker());
                    g2.draw(r);
                } else if (squares[i][j].getType() == Square.WALL) {
                    Color c = Color.darkGray;
                    g2.setColor(c);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(Color.lightGray);
                    g2.draw(r);
                } else if (squares[i][j].getType() == Square.RED) {
                    Color c = new Color(255, 160, 160);
                    g2.setColor(c);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(c.darker());
                    g2.draw(r);
                } else if (squares[i][j].getType() == Square.GREEN) {
                    Color c = new Color(160, 255, 160);
                    g2.setColor(c);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(c.darker());
                    g2.draw(r);
                } else if (squares[i][j].getType() == Square.BLUE) {
                    Color c = new Color(160, 160, 255);
                    g2.setColor(c);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(c.darker());
                    g2.draw(r);
                } else if (squares[i][j].getType() == Square.GOAL) {
                    goalIcon.paintIcon(this, g2, r.x+1, r.y+1);
                    g2.setColor(Color.darkGray);
                    g2.draw(r);
                } else {
                    g2.setColor(Color.red);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(Color.white);
                    g2.drawString("BAD: "+squares[i][j].getType(), r.x, r.y+10);
                }
            }
        }
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(squares.length, squares[0].length);
    }
    
    // ACCESSORS AND MUTATORS
    
    public ImageIcon getGoalIcon() {
        return goalIcon;
    }

    public void setGoalIcon(ImageIcon goalIcon) {
        this.goalIcon = goalIcon;
    }

    public int getSquareWidth() {
        return squareWidth;
    }

    public void setSquareWidth(int squareWidth) {
        this.squareWidth = squareWidth;
    }

    public Square getSquare(int x, int y) {
        return squares[x][y];
    }
    
    public int getFieldWidth() {
        return squares.length;
    }

    public int getFieldHeight() {
        return squares[0].length;
    }
}