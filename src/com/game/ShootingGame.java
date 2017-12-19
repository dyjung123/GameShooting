package com.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ShootingGame extends JFrame{
//    boolean keyExceuted = false;
    Container c;    // c에 접근하기 용이
    Enemy enemy;   // enemy plane에 접근 용이
    Thread th;     // thread에 접근 용이
    public ShootingGame() {
        setTitle("Shooting Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500,360);
        c = getContentPane();

        enemy = new Enemy(new ImageIcon("images/enemy_plane.png"));  // 이미지를 가진 enemy label 생성
        Player player = new Player(new ImageIcon("images/player.png"));  // 이미지를 가진 player label 생성

        c.add(player, BorderLayout.SOUTH);         // 각각 컨테이너에 부착
        c.add(enemy, BorderLayout.NORTH);         // JFrame의 기본 배치관리자는 BorderLayout이며 배치위치는 Center가 기본값
        c.addKeyListener(new KeyAdapter() {
            boolean leftControl = true;    // 하나의 입력만을 받기 위한 key
            boolean rightControl = true;   //  하나의 입력만을 받기 위한 key
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_LEFT && leftControl && rightControl /*&& !keyExceuted*/) { // 한쪽으로 움직이고 있으면
                    leftControl = false;                                                  // 실행x, 스레드 생성x
                    player.pSpeed = -5;
                    th = new Thread(player);
                    th.start();
//                    keyExceuted = th.isAlive();
                } else if(e.getKeyCode() == KeyEvent.VK_RIGHT && rightControl && leftControl /*&& !keyExceuted*/) { // 한쪽으로 움직이면
                    rightControl = false;                                                         // 일단 이동이 멈춰야
                    player.pSpeed = 5;                                                              // 실행.
                    th = new Thread(player);
                    th.start();
//                    keyExceuted = th.isAlive();
                }
                if(e.getKeyCode() == KeyEvent.VK_SPACE) {             // space를 눌러 총알 발사.
                    Bullet bul = new Bullet(new ImageIcon("images/bullet.png"));  // 총알 객체 생성
                    bul.setSize(16,16);                                       // 사이즈 결정
                    bul.setLocation(player.getX()+c.getWidth()/2-10,player.getY());     // 위치 결정, 위치는 플레이어
                    c.add(bul);                 // 컨테이너에 부착
                    c.repaint();                // 새로 부착된 불렛을 표시함.
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {  // key를 떼면 실행
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) { // 뗀 키가 <LEFT>나
                    th.interrupt();       // player move thread 종료시킴                               <RIGHT>면 실행
//                    keyExceuted = false;
                    leftControl = true;   // move input을 받을 수 있는 상태로 전환
                    rightControl = true;  // move input을 받을 수 있는 상태로 전환
                }
            }
        });
        setVisible(true);
        c.requestFocus();
    }
    class Enemy extends JLabel implements Runnable{
        int speed;
        public Enemy(ImageIcon enemyplane) {
            super(enemyplane);
            this.setSize(64,64);
            this.setVerticalAlignment(TOP);  // 위쪽으로 정렬
            this.setHorizontalAlignment(LEFT);   // 왼쪽으로 정렬
            Thread th = new Thread(this);   // enemy plane move thread 생성, 실행
            th.start();
        }

        @Override
        public void run() {
            speed = -5;  // 5픽셀의 속도로 움직임
            int homeX = this.getX();
            int homeY = this.getY();
            while (true) {
                this.setLocation(homeX + speed, homeY);
                if (homeX + speed > -60) {   // 프레임 밖으로 나가면 반대쪽으로 들어옴.
                    speed -= 5;
                } else {
                    speed = 0;
                }
                homeX = c.getWidth();
                try {
                    Thread.sleep(20);  // 0.02초마다 움직임.
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
    class Player extends JLabel implements Runnable {
        int pSpeed;
        public Player(ImageIcon controlPlane) {
            super(controlPlane);
            this.setSize(64,64);
            this.setVerticalAlignment(BOTTOM);
            this.setHorizontalAlignment(CENTER);   // 이 정렬을 사용시 setLocation에 사용되는 기준 좌표자체가 움직임. 주의.
        }
        @Override
        public void run() {
            int pXPosition = this.getX();
            int pYPosition = this.getY();
            while (true) {
                pXPosition += pSpeed;    // pSpeed의 속도로 움직임.
                this.setLocation(pXPosition, pYPosition);
                if (pXPosition <= -c.getWidth()/2) {   // frame 밖으로 나가면 반대편으로 들어옴.
                    pXPosition = c.getWidth()/2;
                } else if (pXPosition > c.getWidth()/2) {  // frame 밖으로 나가면 반대편으로 들어옴.
                    pXPosition = -c.getWidth()/2;
                }
                try {
                    Thread.sleep(25); // 0.025초마다 움직임.
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
    class Bullet extends JLabel implements Runnable {
        Thread bulletthread;
        public Bullet(ImageIcon bullet) {
            super(bullet);
            Thread bulletthread = new Thread(this);  // 총알을 움직이는 thread 생성, 실행
            bulletthread.start();
        }
        @Override
        public void run() {
            int bulXPosition = this.getX();  // bullet객체의 생성위치는 player의 위치.
            int bulYPosition = this.getY();
            while(true) {
                bulYPosition -= 10;  // 위쪽으로 10의 속도로 움직임.
                this.setLocation(bulXPosition,bulYPosition);
                if(bulYPosition <= enemy.getY() + 50 && bulYPosition >= enemy.getY()) {
                    if(bulXPosition <= enemy.getX() + 48 && bulXPosition >= enemy.getX()) { // enemy 격추 판정시
                        enemy.speed = 200;     // enemy는 원래자리에서 사라진것처럼 보인후 다시 날아옴.
                        c.remove(this);  // enemy 격추 bullet 제거.
                        break;                  // bullet move thread 종료.
                    }
                }
                if (bulYPosition < -16) {       // bullet이 frame 위쪽 바깥으로 나가면 thread 종료
                    c.remove(this);
                    break;
                }
                try {
                    Thread.sleep(25);   // 0.025초마다 움직임
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
    public static void main(String[] args) {
        new ShootingGame();
    }
}
