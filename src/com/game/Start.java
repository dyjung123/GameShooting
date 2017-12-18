package com.game;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

public class Start {

    JFrame jf;

    public static void main(String[] args) {
        new Start();
    }

    public Start() {
        EventQueue.invokeLater(new Runnable() { // https://www.euler.kr/trl/2014/02/15/swing-event-dispatch-thread.html
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());  // 자바 스윙에 스킨을 바꿔주는 setLookAndFeel(클래스 이름/경로)
                } catch (ClassNotFoundException | InstantiationException |
                        IllegalAccessException | UnsupportedLookAndFeelException ex) { // LookAndFeel은 이러한 multi-catch를 필요로 한다.
                    ex.printStackTrace(); // printStackTrace()는 에러메세지의 발생 근원지를 찾아서 단계별로 에러를 출력한다.
                }

                MenuModel model = new DefaultMenuModel();
                DefaultMenuView view = new DefaultMenuView();
                MenuViewController controller = new DefaultMenuViewController(model, view);

                jf = new JFrame("Shooting Game");
                jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jf.add(view);
                jf.pack();    // 구성요소의 크기에 맞춰서 frame의 크기를 조절.
                jf.setLocationRelativeTo(null);    // 매개변수 안의 컴포넌트에 따라 frame의 상대적 위치 지정, null이면 가운데.
                jf.setVisible(true);

                controller.start();
            }
        });
    }

    public interface Entity {
        public void paint(Graphics2D g2d);  // 2D 이미지를 그림, entity 생성
        public Point getLocation();         // 위치를 알아냄
        public void setLocation(Point p);   // 위치를 결정
        public Dimension getSize();         // 크기를 알아냄
    }

    public interface MenuModel {
        public Entity[] getEntities();     // entity가 있는 배열을 가져옴
        public void update(Rectangle bounds); // 이미지와 명령 집합을 받아 update
    }

    public interface MenuView {
        public void setController(MenuViewController controller); // 상호작용 할 View를 받아옴.
        public MenuViewController getController();   // controller를 가져옴,알아냄.
        public Rectangle getViewBounds();  // Rectangle(Frame)의 크기와 위치를 가져옴,알아냄.
        public void repaint();  //  MenuView를 다시 그려줌
    }

    public interface MenuViewController {
        public Entity[] getEntities();
        public void start();     // 시작함.
    }

    public class DefaultMenuModel implements MenuModel {  /* 뷰에서 작용하는 객체들 관리 */

        private final List<Entity> entities; // Entity를 담는 상수 List 필드
        private Background background1;
        private Background background2; // 배경2
        private Plane plane;

        private Long lastPlane;

        public DefaultMenuModel() {  // 용량이 25인 Entity를 담는 ArrayList
            entities = new ArrayList<>(25);
        }

        @Override
        public Entity[] getEntities() {  // 특정 실체를 반환.
            return entities.toArray(new Entity[0]); // entities 리스트를 배열로 변환
        }

        @Override
        public void update(Rectangle bounds) {
            if (lastPlane == null) {
                lastPlane = System.currentTimeMillis();
            }
            if (background1 == null) {  // 배경이 없으면 생성
                background1 = new Background(bounds);
                entities.add(background1);
            }
            if (background2 == null) {  // 배경이 없으면 생성
                background2 = new Background(bounds,new Point(0,-2000));
                entities.add(background2);
            }
            if (System.currentTimeMillis() - lastPlane > 3000) {
                lastPlane = System.currentTimeMillis();
                plane = new Plane(bounds);
                entities.add(plane);
            }

            int yPlaneDel = 4;
            int yDelta = 1;    // y축 움직임

            Iterator<Entity> it = entities.iterator(); // entities List의 요소에 모두 접근하기 위한 Iterator
            while (it.hasNext()) {
                Entity entity = it.next();
                if (entity instanceof Background) {
                    Point location = entity.getLocation();
                    location.y += yDelta;
                    if (location.y > bounds.y + bounds.height) {
                        entity.setLocation(new Point(0,-1996));
                    } else {
                        entity.setLocation(location);
                    }
                }

                if (entity instanceof Plane) {
                    Point pLocation = entity.getLocation();
                    Dimension pSize = entity.getSize();
                    pLocation.y -= yPlaneDel;
                    if (pLocation.y < bounds.y - pSize.height) {
                        it.remove();
                    } else {
                        entity.setLocation(pLocation);
                    }
                }
            }
        }

    }

    public class DefaultMenuViewController implements MenuViewController { // 기본게임 컨트롤러

        private MenuModel model;  // GameModel
        private MenuView view;  // GameView

        private Timer timer;  // Swing Timer

        public DefaultMenuViewController(MenuModel menuModel, MenuView menuView) { // GameModel과 GameView를 받아 DefaultGameController 생성.
            menuView.setController(this);  // 받아온 GameView에 Controller 세팅.
            view = menuView;
            model = menuModel;
        }

        @Override
        public Entity[] getEntities() {
            return model.getEntities();
        }

        @Override
        public void start() {
            if (timer != null && timer.isRunning()) {  // timer가 비어있거나 실행중이면 종료.
                timer.stop();  // Swing Timer는 반드시 timer.stop()으로 종료시켜야함.
            }
            timer = new Timer(40, new ActionListener() {  // 0.04초마다 Action 수행.
                @Override
                public void actionPerformed(ActionEvent e) {  // 프레임의 위치,크기와 명령어 받은 View를 계속 업데이트함.
                    model.update(view.getViewBounds());  // 프레임의 위치,크기와 변경 불가능한 명령어를 받아 GameModel을 update
                    view.repaint();  // view를 다시그림.
                }
            });
            timer.start();
        }
    }

    public abstract class AbstractEntity implements Entity { // Entity의 위치를 get하고, set하는 추상 클래스

        private final Point location = new Point(0,0);  // location이 참조하는 객체는 바뀌지 않음.

        @Override
        public Point getLocation() {  // 위치정보를 받아옴.
            return new Point(location);
        }

        @Override
        public void setLocation(Point p) { // setLocation은 위치정보 객체를 담는 location의 참조를 바꾸는것이 아닌 참조하고 있는 객체의 내용을 바꿈.
            location.setLocation(p);   // 위치정보를 결정,세팅.
        }

    }

    public class Background extends AbstractEntity {

        public Background(Rectangle bounds, Point point) {
            setLocation(point);
        }

        public Background(Rectangle bounds) {
            int x = bounds.x;
            int y = bounds.y - (getSize().height - bounds.height);
            setLocation(new Point(x,y));
        }

        @Override
        public void paint(Graphics2D g2d) {
            Point p = getLocation();
            Dimension size = getSize();

            ImageIcon icon = new ImageIcon("images/iceBG.png");
            Image img = icon.getImage();
            g2d.drawImage(img,p.x,p.y,size.width,size.height,null);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(400, 1200);
        }
    }

    public class Plane extends AbstractEntity {

        public Plane(Rectangle bounds) {
            Random random = new Random();
            int x = bounds.x + random.nextInt(bounds.width - (getSize().width * 2)) + getSize().width;
            int y = bounds.y + bounds.height;
            setLocation(new Point(x,y));
        }

        @Override
        public void paint(Graphics2D g2d) {
            Point p = getLocation();
            Dimension size = getSize();

            ImageIcon icon = new ImageIcon("images/player.png");
            Image img = icon.getImage();
            g2d.drawImage(img,p.x,p.y,size.width,size.height,null);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(30, 13);
        }
    }

    public class DefaultMenuView extends JPanel implements MenuView {

        private MenuViewController controller;

        public DefaultMenuView() {
            setLayout(null);
            JButton btn = new JButton("시작");
            JButton btn2 = new JButton("종료");
            btn.setBounds(150,135,100,60);
            btn2.setBounds(150,205,100,60);
            btn.setFont(new Font("고딕",Font.ITALIC,20));
            btn2.setFont(new Font("고딕",Font.ITALIC,20));
            btn.addActionListener(new MyActionListener());
            btn2.addActionListener(new MyActionListener());
            add(btn);
            add(btn2);
        }

        @Override
        public void setController(MenuViewController controller) {
            this.controller = controller;
        }

        @Override
        public MenuViewController getController() {
            return controller;
        }

        @Override
        public Rectangle getViewBounds() {
            return new Rectangle(new Point(0,0), getSize());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(400, 400);
        } //  참조되고 있는 프레임의 size

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            MenuViewController controller = getController();
            for(Entity entity : controller.getEntities()) {
                Graphics2D g2d = (Graphics2D) g.create();
                entity.paint(g2d);
                g2d.dispose();
            }
        }

        private class MyActionListener implements ActionListener {  // JButton Listener
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton b = (JButton)e.getSource();
                if(b.getText().equals("시작")) {
                    new MyGame();
                    jf.dispose();
                } else if(b.getText().equals("종료")) {
                    System.exit(0);
                }
            }
        }
    }
}
