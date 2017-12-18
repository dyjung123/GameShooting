package com.game;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public class MyGame {

    public static void main(String[] args) {
        new MyGame();
    }

    public MyGame() {                           // Event Dispatching Thread에서 처리하기 위해 invokeLater 메소드를 사용.
        EventQueue.invokeLater(new Runnable() { // https://www.euler.kr/trl/2014/02/15/swing-event-dispatch-thread.html
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());  // 자바 스윙에 스킨을 바꿔주는 setLookAndFeel(클래스 이름/경로)
                } catch (ClassNotFoundException | InstantiationException |
                        IllegalAccessException | UnsupportedLookAndFeelException ex) { // LookAndFeel은 이러한 multi-catch를 필요로 한다.
                    ex.printStackTrace(); // printStackTrace()는 에러메세지의 발생 근원지를 찾아서 단계별로 에러를 출력한다.
                }

                GameModel model = new DefaultGameModel();  // 게임객체들의 생성 및 이동을 관리.
                DefaultGameView view = new DefaultGameView();  // JPanel을 상속받은 View, 화면 표시 및 명령입력을 받음.
                GameController controller = new DefaultGameController(model, view);

                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(view);
                frame.pack();  // 구성요소의 크기에 맞춰서 frame의 크기를 조절.
                frame.setLocationRelativeTo(null);  // 매개변수 안의 컴포넌트에 따라 frame의 상대적 위치 지정, null이면 가운데.
                frame.setVisible(true);

                controller.start();
            }
        });
    }

    public static enum Direction { // 명령의 이름 설정.
        LEFT,
        RIGHT,
        UP,
        DOWN,
        SPACE
    }

    public interface Entity {
        public void paint(Graphics2D g2d);  // 2D 이미지를 그림, entity 생성
        public Point getLocation();         // 위치를 알아냄
        public void setLocation(Point p);   // 위치를 결정
        public Dimension getSize();         // 크기를 알아냄
    }

    public interface GameModel {
        public Player getPlayer();
        public Entity[] getEntities();     // entity가 있는 배열을 가져옴
        public void update(Rectangle bounds, Set<Direction> keys); // 이미지와 명령 집합을 받아 update
    }

    public interface GameController {
        public Entity[] getEntities();    // entity가 있는 배열을 가져옴
        public void setDirection(Direction direction, boolean pressed); // 눌려진키가 뭔지, 눌려졌는지를 받아 명령결정.
        public void start();     // 시작함.
        public void stop();
        public GameModel getModel();
    }

    public interface GameView {
        public void setController(GameController controller); // controller를 받아와 GameView에 표시
        public GameController getController();   // controller를 가져옴,알아냄.
        public Rectangle getViewBounds();  // Rectangle(Frame)의 크기와 위치를 가져옴,알아냄.
        public void repaint();  //  GameView를 다시 그려줌
    }

    public class DefaultGameModel implements GameModel {  /*  플레이어를 비롯한 게임 객체들의 생성과 움직임을 관리. */

        private final List<Entity> entities; // Entity를 담는 상수 List 필드
        private Player player;  // 플레이어 객체를 담는 필드
        private final ArrayList<Enemy> enemies; // Enemy를 담는 상수 ArrayList 필드
        private Enemy enemy;  // enemy 객체를 참조할 변수
        private Background background1; // 배경1 , 배경1과 배경2는 끊기지 않는 배경연결을 위한것으로 결국 같은 배경.
        private Background background2; // 배경2

        private Long lastShot; // bullet이 발사된 시간을 System.currentTimeMillis()로 받아와 저장.
        private Long enemyCreateCoolTime;

        public DefaultGameModel() {  // 용량이 25인 Entity를 담는 ArrayList
            entities = new ArrayList<>(25);
            enemies = new ArrayList<>(10);
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public Entity[] getEntities() {  // 특정 실체를 반환.
            return entities.toArray(new Entity[0]); // entities 리스트를 배열로 변환
        }

        @Override
        public void update(Rectangle bounds, Set<Direction> keys) {
            if (enemyCreateCoolTime == null) {
                enemyCreateCoolTime = System.currentTimeMillis();
            }
            if (background1 == null) {  // 배경이 없으면 생성
                background1 = new Background(bounds);
                entities.add(background1);
            }
            if (background2 == null) {  // 배경이 없으면 생성
                background2 = new Background(bounds,new Point(0,-2000));
                entities.add(background2);
            }
            if (player == null) {  // player가 존재하지 않으면
                player = new Player(bounds);  // 위치,사이즈를 받아 Player형 객체를 생성
                entities.add(player); // entities 리스트에 player 객체 추가
            }
            if (enemy == null || System.currentTimeMillis() - enemyCreateCoolTime > 2500) {   // enemy 객체 생성
                enemyCreateCoolTime = System.currentTimeMillis();
                if (enemies.size() < 5) {
                    enemy = new Enemy(bounds);
                    entities.add(enemy);
                    enemies.add(enemy);
                }
            }

            Point p = player.getLocation(); // player의 위치 정보를 저장

            int xDelta = 0; // x축으로의 변화량, x축으로의 움직임을 담당
            if (keys.contains(Direction.LEFT)) {  // 전달받은 keys가 LEFT이면 왼쪽으로 움직임.
                xDelta = -4;
            } else if (keys.contains(Direction.RIGHT)) {  // 전달받은 keys가 RIGHT이면 오른쪽으로 움직임.
                xDelta = 4;
            }

            int yDelta = 0;    // y축 움직임 , UP or DOWN
            if (keys.contains(Direction.UP)) {
                yDelta = -4;
            } else if (keys.contains(Direction.DOWN)) {
                yDelta = 4;
            }

            int backgroundYDelta = 4;

            p.x += xDelta; // 플레이어의 x좌표를 xDelta만큼 변경,움직임.
            p.y += yDelta;

            if (p.x <= bounds.x) {  // 플레이어가 프레임 밖으로 못나가게 함.
                p.x = bounds.x;  // bounds.x : 프레임 자체의 화면상 x축 절대좌표.
            } else if (p.x + player.getSize().width >= bounds.x + bounds.width) {  // 플레이어가 화면밖으로 못나가게함.
                p.x = bounds.width - player.getSize().width;
            }

            if (p.y <= bounds.y) {    // player가 frame 바깥으로 나가지 못하게 함
                p.y = bounds.y;
            } else if (p.y + player.getSize().height >= bounds.y + bounds.height) {
                p.y = bounds.height - player.getSize().height;
            }

            player.setLocation(p); // player의 위치를 p로 결정

            Iterator<Entity> it = entities.iterator(); // entities List의 요소에 모두 접근하기 위한 Iterator
            while (it.hasNext()) {
                Entity entity = it.next();
                if (entity instanceof Bullet) { // entity가 Bullet으로 형변환이 가능하다면. bullet이면 날아감.
                    Point location = entity.getLocation();  // entity의 위치를 받아옴.
                    Dimension size = entity.getSize();      // entity의 크기를 받아옴
                    location.y -= size.height;    // entity의 위치에서 entity의 높이만큼을 빼서 y축 좌표정보에 넣어줌
                    if (location.y + size.height < bounds.y) { // 탄환이 frame 바깥으로 나가면 제거
                        it.remove();
                    } else {   // 그렇지 않으면 entity의 위치정보를 location으로 갱신해줌. 즉, Bullet은 위로 날아감
                        entity.setLocation(location);
                    }
                    for (int i=0; i<enemies.size(); i++) {
                        if (location.x < enemies.get(i).getLocation().x + enemies.get(i).getSize().width - 4 && location.x > enemies.get(i).getLocation().x - 3 &&
                                location.y < enemies.get(i).getLocation().y + enemies.get(i).getSize().height - 2 && location.y > enemies.get(i).getLocation().y + 2) {  // 적에게 총알이 맞았으면
                            it.remove();  // 총알 제거
                            enemies.get(i).setHit();  // 적이 총알에 맞았음을 표시
                        }
                    }
                }
                if (entity instanceof EnemyBullet) { // entitu가 EnemyBullet으로 형변환이 가능하다면, enemy가 발사한 bullet이면 날아감.
                    Point eLocation = entity.getLocation();  // EnemyBullet의 위치를 받아옴.
                    Dimension eSize = entity.getSize();      // EnemyBullet의 크기를 받아옴.
                    eLocation.y += eSize.height - 2; // ????????????
                    if (eLocation.y > bounds.y + bounds.height) {
                        it.remove();
                    } else if(eLocation.x < player.getLocation().x + player.getSize().width - 4 && eLocation.x > player.getLocation().x - 3 &&
                            eLocation.y < player.getLocation().y + player.getSize().height - 2 && eLocation.y > player.getLocation().y + 2) {  // player에게 총알이 맞았으면
                        it.remove();
                        player.setHit();
                    } else {   // enemyBullet은 특정 방향으로 날아감.
                        entity.setLocation(eLocation);
                    }
                }
                if (entity instanceof Enemy) {
                    Enemy tempEnemy = (Enemy) entity;
                    Point enemyLocation = entity.getLocation(); // 적의 위치
                    if (!tempEnemy.getInFrame && enemyLocation.y > bounds.y) {  // 생성후 프레임 밖에서 enemy가 프레임 안으로 들어오면 속도 변경
                        tempEnemy.getInFrame = true;
                        int x = (int)(Math.random() * 9) - 4; // x축 속도 -4 ~ 4
                        int y = (int)(Math.random() * 2) + 3; // y축 속도 3 ~ 4
                        tempEnemy.setSpeed(x,y);
                    }

                    enemyLocation.x += tempEnemy.getSpeedX();
                    enemyLocation.y += tempEnemy.getSpeedY();
                    if (enemyLocation.x <= bounds.x) {   // enemy가 frame 바깥으로 나가지 못하게 함
                        enemyLocation.x = bounds.x;
                        Random random = new Random();
                        int exDelta = random.nextInt(1) + 3;  // x축 속도 3~4중 랜덤 결정
                        int eyDelta = random.nextInt(8) - 4;  // y축 속도 -4~4중 랜덤 결정
                        tempEnemy.setSpeed(exDelta, eyDelta);  // enemy 속도 결정
                    } else if (enemyLocation.x + tempEnemy.getSize().width >= bounds.x + bounds.width) {
                        enemyLocation.x = bounds.width - tempEnemy.getSize().width;
                        Random random = new Random();
                        int exDelta = random.nextInt(1) - 4;  // x축 속도 -3 ~ -4중 랜덤 결정
                        int eyDelta = random.nextInt(8) - 4;  // y축 속도 -4~4중 랜덤 결정
                        tempEnemy.setSpeed(exDelta, eyDelta);  // enemy 속도 결정
                    }

                    if (enemyLocation.y <= bounds.y && tempEnemy.getInFrame) {    // enemy가 frame 바깥으로 나가지 못하게 함
                        enemyLocation.y = bounds.y;
                        Random random = new Random();
                        int exDelta = random.nextInt(8) - 4;  // x축 속도 -4~4중 랜덤 결정
                        int eyDelta = random.nextInt(1) + 3;  // y축 속도 3~4중 랜덤 결정
                        tempEnemy.setSpeed(exDelta, eyDelta);  // enemy 속도 결정
                    } else if (enemyLocation.y + tempEnemy.getSize().height >= bounds.y + bounds.height) {
                        enemyLocation.y = bounds.height - tempEnemy.getSize().height;
                        Random random = new Random();
                        int exDelta = random.nextInt(8) - 4;  // x축 속도 -4~4중 랜덤 결정
                        int eyDelta = random.nextInt(1) - 4;  // y축 속도 -3 ~ -4중 랜덤 결정
                        tempEnemy.setSpeed(exDelta, eyDelta);  // enemy 속도 결정
                    }
                    tempEnemy.setLocation(enemyLocation); // enemy의 위치를 ep결정
                }
                if (entity instanceof BoomEffect) {  // entity가 BoomEffect이면
                    if (System.currentTimeMillis() - ((BoomEffect) entity).getBoomTime() > 1000) { // 터진지 1초가 지났으면 폭발효과 제거.
                        it.remove();
                    }
                }
                if (entity instanceof Background) {
                    Point location2 = entity.getLocation();
                    location2.y += backgroundYDelta;
                    if (location2.y > bounds.y + bounds.height) {
                        entity.setLocation(new Point(0,-1996));
                    } else {
                        entity.setLocation(location2);
                    }
                }
            }
            for (int i=0; i<enemies.size(); i++) {
                Enemy eee = enemies.get(i);
                if (eee.getHit()) {  // 적기를 총알에 맞았으면 제거.
                    BoomEffect boomEffect = new BoomEffect(); // 폭발 효과 생성.
                    boomEffect.setLocation(eee.getLocation()); // 폭발효과의 위치는 적의 위치.
                    entities.remove(eee);  // enemy 파괴,제거.
                    enemies.remove(i);  // 참조를 null로 바꿔서 메모리에서 완전 제거.
                    boomEffect.setBoomTime(System.currentTimeMillis()); // 폭발이 생긴 시간을 저장.
                    entities.add(boomEffect);  // 폭발효과 객체를 entities List에 추가.
//                    eee = null;
                }
                if (System.currentTimeMillis() - eee.getShotTime() > 300) {
                    eee.setShotTime(System.currentTimeMillis());
                    EnemyBullet ebullet = new EnemyBullet();
                    int ex = eee.getLocation().x + ((eee.getSize().width - ebullet.getSize().width) / 2);
                    int ey = eee.getLocation().y + eee.getSize().height;
                    ebullet.setLocation(new Point(ex, ey));

                    entities.add(ebullet);
                }
            }

            if (player.getHit()) {  // 플레이어를 총알에 맞았으면 제거.
                player.setHit();
                player.setLife(-1);
                BoomEffect boomEffect = new BoomEffect(); // 폭발 효과 생성.
                boomEffect.setLocation(player.getLocation()); // 폭발효과의 위치는 적의 위치.
                boomEffect.setBoomTime(System.currentTimeMillis()); // 폭발이 생긴 시간을 저장.
                entities.add(boomEffect);  // 폭발효과 객체를 entities List에 추가.
                if (player.getLife() == 0) {
                    entities.remove(player);  // player 파괴,제거.
                }
            }

            if (player != null && keys.contains(Direction.SPACE)) {  // 입력받은 명력이 SPACE라면 bullet생성
                if (lastShot == null || System.currentTimeMillis() - lastShot > 150) { // 발사 쿨타임보다 쏜지 오래됬거나 쏜적이 없으면
                    lastShot = System.currentTimeMillis(); // 마지막으로 쏜시간을 lastShot에 저장.
                    Bullet bullet = new Bullet(); // 발사 Bullet 생성.
                    int x = p.x + ((player.getSize().width - bullet.getSize().width) / 2);  // bullet 이미지가 player의 너비 중간에 보이도록 x좌표 설정.
                    int y = p.y - bullet.getSize().height; // bullet 이미지가 플레이어 바로 바깥에서 생성되도록 y좌표 설정.
                    bullet.setLocation(new Point(x, y)); // bullet의 위치 설정.

                    entities.add(bullet); // entities에 생성된 bullet을 추가.
                }
            }
        }

    }

    public class DefaultGameController implements GameController { // 기본게임 컨트롤러

        private GameModel model;  // GameModel
        private GameView view;  // GameView

        private Timer timer;  // Swing Timer

        private Set<Direction> keys = new HashSet<>(25); // 용량이 25인 명령을 담는 HashSet을 생성

        public DefaultGameController(GameModel gameModel, GameView gameView) { // GameModel과 GameView를 받아 DefaultGameController 생성.
            gameView.setController(this);  // 받아온 GameView에 Controller 세팅.

            view = gameView;
            model = gameModel;
        }

        @Override
        public Entity[] getEntities() {  // 게임 모델들의 Entity 리스트를 반환.
            return model.getEntities();
        }

        @Override
        public void setDirection(Direction direction, boolean pressed) { // 명령과 입력여부를 받아와 실행.
            if (pressed) {  // 눌러졌으면 명령 집합 keys에 추가.
                keys.add(direction);
            } else { // 안눌러졌으면 명령 제거.
                keys.remove(direction);
            }
        }

        @Override
        public void start() {
            if (timer != null && timer.isRunning()) {  // timer가 비어있거나 실행중이면 종료.
                timer.stop();  // Swing Timer는 반드시 timer.stop()으로 종료시켜야함.
            }
            timer = new Timer(40, new ActionListener() { // 0.04초마다 Action 수행.
                @Override
                public void actionPerformed(ActionEvent e) {  // 프레임의 위치,크기와 명령어 받은 View를 계속 업데이트함.
//                    if (model.getPlayer() != null && model.getPlayer().getLife() == 0) {
//                        timer.stop();
//                    }
                    model.update(view.getViewBounds(), Collections.unmodifiableSet(keys));  // 프레임의 위치,크기와 명령 집합 keys를 변경불가능 집합으로 전달, GameModel을 update
                    view.repaint();  // view를 다시그림.
                }
            });
            timer.start();
        }

        @Override
        public void stop() {
            timer.stop();
        }

        @Override
        public GameModel getModel() {
            return model;
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

    public class Player extends AbstractEntity { // 위치 좌표를 변경하고 알아낼수있는 Player 객체

        private boolean hit = false;  // 맞았는지 표시
        private int life;

        public Player(Rectangle bounds) {  // 초기 player 위치.
            this.life = 3;
            int x = bounds.x + ((bounds.width - getSize().width) / 2);  // x좌표는 프레임 딱 중간
            int y = bounds.y + (bounds.height - getSize().height);  // y좌표는 프레임 밑바닥.
            setLocation(new Point(x, y));  // 위치정보 설정.
        }

        public void setLife(int num) { this.life += num; }

        public int getLife() { return this.life; }

        public void setHit() { // 맞았으면 setHit 호출
            if (getHit() == false) {
                this.hit = true;
            } else {
                this.hit = false;
            }
        }

        public boolean getHit() {
            return this.hit;
        }  // 맞았는지 판단.

        @Override
        public Dimension getSize() {  // size 반환
            return new Dimension(40, 17);  // Player 캐릭터의 size
        }

        @Override
        public void paint(Graphics2D g2d) {
            Point p = getLocation();   // player의 location
            Dimension size = getSize();  // player의 size

            ImageIcon icon = new ImageIcon("images/player.png");
            Image img = icon.getImage();
            g2d.drawImage(img,p.x,p.y,size.width,size.height,null);
        }

    }

    public class Enemy extends AbstractEntity {  // Enemy 객체

        private int exDelta;  // x축 속도
        private int eyDelta;  // y축 속도
        private boolean hit;  // 맞았는지 표시
        private long enemyLastShot = 0;
        boolean getInFrame = false;

        public Enemy(Rectangle bounds) {
            Random random = new Random();
            int x = bounds.x + random.nextInt(bounds.width - getSize().width - 2) + 1;
            int y = bounds.y - getSize().height;
            setLocation(new Point(x, y));
            this.exDelta = 0;                           // 생성시 x축 속도 : 0
            this.eyDelta = 2;/*(int)(Math.random()*5)*/;      // 생성시 y축 속도 : 범위 0 ~ 4
            this.hit = false;  // 맞았는지 설정
        }

        public void setHit() {
            this.hit = true;
        }  // 맞았으면 setHit 호출

        public boolean getHit() {
            return this.hit;
        }  // 맞았는지 판단.

        public void setShotTime(long ShotTime) { this.enemyLastShot = ShotTime; }

        public long getShotTime() { return enemyLastShot; }

        public void setSpeed(int xDel, int yDel) {  // 속도 설정
            this.exDelta = xDel;
            this.eyDelta = yDel;
        }

        public int getSpeedX() {  // x축 속도 반환
            return this.exDelta;
        }

        public int getSpeedY() {  // y축 속도 반환
            return this.eyDelta;
        }
        @Override
        public void paint(Graphics2D g2d) {
            Point p = getLocation();
            Dimension size = getSize();

            ImageIcon icon = new ImageIcon("images/enemy_plane.png");
            Image img = icon.getImage();
            g2d.drawImage(img,p.x,p.y,size.width,size.height,null);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(40, 17);
        }
    }

    public class Bullet extends AbstractEntity { // 위치 좌표를 변경하고 알아낼수있는 Bullet 객체

        @Override
        public void paint(Graphics2D g2d) {
            Rectangle bullet = new Rectangle(getLocation(), getSize());  // 위치와 사이즈를 설정해서 bullet객체 생성.
            g2d.setColor(Color.RED);
            g2d.fill(bullet);
//            ImageIcon icon = new ImageIcon("images/bullet.png");
//            Image img = icon.getImage();
//            g2d.drawImage(img,getLocation().x,getLocation().y,getSize().width,getSize().height,null);
        }

        @Override
        public Dimension getSize() { // Bullet의 사이즈
            return new Dimension(4, 8);
        }

    }

    public class EnemyBullet extends AbstractEntity {  // 적기가 발사하는 총알

        @Override
        public void paint(Graphics2D g2d) {
            Rectangle bullet = new Rectangle(getLocation(), getSize());  // 위치와 사이즈를 설정해서 bullet객체 생성.
            g2d.setColor(Color.blue);
            g2d.fill(bullet);
        }

        @Override
        public Dimension getSize() { // Bullet의 사이즈
            return new Dimension(4, 8);
        }
    }

    public class BoomEffect extends AbstractEntity {  // 폭발효과

        private long boomTime = 0; // bullet이 맞아서 터진 시간을 System.currentTimeMillis()로 받아와 저장.

        public void setBoomTime(long time) {
            this.boomTime = time;
        }

        public long getBoomTime() {
            return boomTime;
        }

        @Override
        public void paint(Graphics2D g2d) {
            ImageIcon icon = new ImageIcon("images/explosion-153710_640.png");
            Image img = icon.getImage();
            g2d.drawImage(img,getLocation().x,getLocation().y,getSize().width,getSize().height,null);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(40,40);
        }
    }

    public class DefaultGameView extends JPanel implements GameView {

        private GameController controller;  // 명령세팅과 게임Entity들의 리스트를 가져오고 게임모델과 View를 지속적으로 업데이트하는 controller

        public DefaultGameView() {  // 각각 명령들 등록
            addKeyBinding("left.pressed", KeyEvent.VK_LEFT, true, new DirectionAction(Direction.LEFT, true));
            addKeyBinding("left.released", KeyEvent.VK_LEFT, false, new DirectionAction(Direction.LEFT, false));
            addKeyBinding("right.pressed", KeyEvent.VK_RIGHT, true, new DirectionAction(Direction.RIGHT, true));
            addKeyBinding("right.released", KeyEvent.VK_RIGHT, false, new DirectionAction(Direction.RIGHT, false));
            addKeyBinding("space.pressed", KeyEvent.VK_SPACE, true, new DirectionAction(Direction.SPACE, true));
            addKeyBinding("space.released", KeyEvent.VK_SPACE, false, new DirectionAction(Direction.SPACE, false));
            addKeyBinding("up.pressed", KeyEvent.VK_UP, true , new DirectionAction(Direction.UP, true));
            addKeyBinding("up.released", KeyEvent.VK_UP, false , new DirectionAction(Direction.UP, false));
            addKeyBinding("down.pressed", KeyEvent.VK_DOWN, true , new DirectionAction(Direction.DOWN, true));
            addKeyBinding("down.released", KeyEvent.VK_DOWN, false , new DirectionAction(Direction.DOWN, false));
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
        public void setController(GameController controller) { // 받은 Controller로 controller 세팅.
            this.controller = controller;
        }

        @Override
        public GameController getController() {
            return controller;
        } // 참조하고 있는 controller를 알려줌 반환.

        @Override
        public Rectangle getViewBounds() {
            return new Rectangle(new Point(0,0), getSize());
        }  // frame의 위치,크기를 반환.

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(400, 400);
        } //  프레임에 부착될 View size

        @Override
        protected void paintComponent(Graphics g) {  // Graphic을 frame에 표시하기 위한 메소드
            super.paintComponent(g);
            GameController controller = getController();
            for(Entity entity : controller.getEntities()) {  // controller.getEntities()는 Entity 리스트를 반환함.
                // i don't trust you ??
                if (controller.getModel().getPlayer().getLife() == 0) {
                    controller.stop();
                    JOptionPane.showMessageDialog(this, "GAME OVER", "", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                Graphics2D g2d = (Graphics2D) g.create();  // i dont know this
                entity.paint(g2d);  // Graphics로 해당 Entity(player,enemy,bullet)에 해당하는 도형, 혹은 이미지를 그림
                g2d.dispose(); /* dispose() 메서드는 그래픽 문맥에서 사용중인 시스템 자원을 해제한다. dispose()를 호출한 후에는
                                 Graphics g를 사용할 수 없다. getGraphics()를 이용해서 Graphics를 얻었다면 반드시 dispose()로 자원을 해제해주어야 한다.
                                  https://m.blog.naver.com/PostView.nhn?blogId=seektruthyb&logNo=150114863254&proxyReferer=https%3A%2F%2Fwww.google.co.kr%2F  */
            }
        }

        public class DirectionAction extends AbstractAction {

            private Direction direction;  // 명령어
            private boolean pressed;     // 누름 여부

            public DirectionAction(Direction direction, boolean pressed) {  // 생성자로 명령이 무엇인지, 눌렀는지를 저장.
                this.direction = direction;
                this.pressed = pressed;
            }

            @Override
            public void actionPerformed(ActionEvent e) { getController().setDirection(direction, pressed); }
            // 받아온 controller에 명령이 입력되었으면 추가(add)하고 아니면 삭제(remove)
        }
    }
}
