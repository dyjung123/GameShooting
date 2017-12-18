package com.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {

    /*main 메소드는 GUI를 구동하는 Runnable 객체를 생성하여, event dispatch thread에서 실행되도록 요청(invoke)하는 코드가 된다.*/
    public static void main(String[] args) {
        new Main();
    }

    public Main() {  // 생성자로 곧바로 실행됨.
        EventQueue.invokeLater(new Runnable() { // invokeLater 쓰레드를 먼저 등록.
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                GameModel model = new DefaultGameModel();
                DefaultGameView view = new DefaultGameView();
                GameController controller = new DefaultGameController(model, view);

                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(view);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                controller.start();
            }
        });
    }

    public static enum Direction {
        LEFT,
        RIGHT,
        SPACE
    }

    public interface Entity {
        public void paint(Graphics2D g2d);
        public Point getLocation();
        public void setLocation(Point p);
        public Dimension getSize();
    }

    public interface GameModel {
        public Entity[] getEntities();
        public void update(Rectangle bounds, Set<Direction> keys);
    }

    public interface GameController {
        public Entity[] getEntities();
        public void setDirection(Direction direction, boolean pressed);
        public void start();
    }

    public interface GameView {
        public void setController(GameController controller);
        public GameController getController();
        public Rectangle getViewBounds();
        public void repaint();
    }

    public class DefaultGameModel implements GameModel {

        private final List<Entity> entities;
        private Player player;

        private Long lastShot; // 이전에 Bullet을 쏜 시간을 저장.

        public DefaultGameModel() {
            entities = new ArrayList<>(25);
        }

        @Override
        public Entity[] getEntities() {
            return entities.toArray(new Entity[0]);
        }

        @Override
        public void update(Rectangle bounds, Set<Direction> keys) {
            if (player == null) {
                player = new Player(bounds);
                entities.add(player);
            }

            Point p = player.getLocation();
            int xDelta = 0;
            if (keys.contains(Direction.LEFT)) {
                xDelta = -4;
            } else if (keys.contains(Direction.RIGHT)) {
                xDelta = 4;
            }
            p.x += xDelta;
            if (p.x <= bounds.x) {
                p.x = bounds.x;
            } else if (p.x + player.getSize().width >= bounds.x + bounds.width) {
                p.x = bounds.width - player.getSize().width;
            }
            player.setLocation(p);

            Iterator<Entity> it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = it.next();
                if (entity instanceof Bullet) {
                    Point location = entity.getLocation();
                    Dimension size = entity.getSize();
                    location.y -= size.height;
                    if (location.y + size.height < bounds.y) {
                        it.remove();
                    } else {
                        entity.setLocation(location);
                    }
                }
            }

            if (keys.contains(Direction.SPACE)) {
                if (lastShot == null || System.currentTimeMillis() - lastShot > 100) {
                    lastShot = System.currentTimeMillis();
                    Bullet bullet = new Bullet();
                    int x = p.x + ((player.getSize().width - bullet.getSize().width) / 2);
                    int y = p.y - bullet.getSize().height;
                    bullet.setLocation(new Point(x, y));

                    entities.add(bullet);
                }
            }
        }

    }

    public class DefaultGameController implements GameController {

        private GameModel model;
        private GameView view;

        private Timer timer;

        private Set<Direction> keys = new HashSet<>(25);

        public DefaultGameController(GameModel gameModel, GameView gameView) {
            gameView.setController(this);

            view = gameView;
            model = gameModel;
        }

        @Override
        public Entity[] getEntities() {
            return model.getEntities();
        }

        @Override
        public void setDirection(Direction direction, boolean pressed) {
            if (pressed) {
                keys.add(direction);
            } else {
                keys.remove(direction);
            }
        }

        @Override
        public void start() {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new Timer(40, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.update(view.getViewBounds(), Collections.unmodifiableSet(keys));
                    view.repaint();
                }
            });
            timer.start();
        }

    }

    public abstract class AbstractEntity implements Entity {

        private final Point location = new Point(0, 0);

        @Override
        public Point getLocation() {
            return new Point(location);
        }

        @Override
        public void setLocation(Point p) {
            location.setLocation(p);
        }

    }

    public class Player extends AbstractEntity {

        public Player(Rectangle bounds) {
            int x = bounds.x + ((bounds.width - getSize().width) / 2);
            int y = bounds.y + (bounds.height - getSize().height);
            setLocation(new Point(x, y));
        }

        @Override
        public Dimension getSize() {
            return new Dimension(40, 17);
        }

        @Override
        public void paint(Graphics2D g2d) {
            Point p = getLocation();
            Dimension size = getSize();
            g2d.setColor(Color.BLUE);
            g2d.fillRect(p.x, p.y + 7, size.width, 10);
            g2d.fillRect(p.x + 18, p.y, 4, 7);
        }

    }

    public class Bullet extends AbstractEntity {

        @Override
        public void paint(Graphics2D g2d) {
            Rectangle bullet = new Rectangle(getLocation(), getSize());
            g2d.setColor(Color.RED);
            g2d.fill(bullet);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(4, 8);
        }

    }

    public class DefaultGameView extends JPanel implements GameView {

        private GameController controller;

        public DefaultGameView() {
            addKeyBinding("left.pressed", KeyEvent.VK_LEFT, true, new DirectionAction(Direction.LEFT, true));
            addKeyBinding("left.released", KeyEvent.VK_LEFT, false, new DirectionAction(Direction.LEFT, false));
            addKeyBinding("right.pressed", KeyEvent.VK_RIGHT, true, new DirectionAction(Direction.RIGHT, true));
            addKeyBinding("right.released", KeyEvent.VK_RIGHT, false, new DirectionAction(Direction.RIGHT, false));
            addKeyBinding("space.pressed", KeyEvent.VK_SPACE, true, new DirectionAction(Direction.SPACE, true));
            addKeyBinding("space.released", KeyEvent.VK_SPACE, false, new DirectionAction(Direction.SPACE, false));
        }

        protected void addKeyBinding(String name, int keyEvent, boolean pressed, DirectionAction action) {
            addKeyBinding(name, KeyStroke.getKeyStroke(keyEvent, 0, !pressed), action);
        }

        protected void addKeyBinding(String name, KeyStroke keyStroke, DirectionAction action) {
            InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();

            inputMap.put(keyStroke, name);
            actionMap.put(name, action);
        }

        @Override
        public void setController(GameController controller) {
            this.controller = controller;
        }

        @Override
        public GameController getController() {
            return controller;
        }

        @Override
        public Rectangle getViewBounds() {
            return new Rectangle(new Point(0, 0), getSize());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(400, 400);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            GameController controller = getController();
            for (Entity entity : controller.getEntities()) {
                // I don't trust you
                Graphics2D g2d = (Graphics2D) g.create();
                entity.paint(g2d);
                g2d.dispose();
            }
        }

        public class DirectionAction extends AbstractAction {

            private Direction direction;
            private boolean pressed;

            public DirectionAction(Direction direction, boolean pressed) {
                this.direction = direction;
                this.pressed = pressed;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                getController().setDirection(direction, pressed);
            }

        }

    }

}