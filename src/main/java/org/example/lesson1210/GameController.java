package org.example.lesson1210;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.*;

/**
 * Контроллер игровой сцены.
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>создание игрока и монет на игровом поле {@link #gamePane};</li>
 *     <li>обработку нажатий клавиш (WASD) для перемещения игрока;</li>
 *     <li>ограничение перемещения игрока границами панели;</li>
 *     <li>обнаружение столкновений игрока с монетами и начисление очков;</li>
 *     <li>обновление кадров через {@link AnimationTimer}.</li>
 * </ul>
 */
public class GameController {

    /** Игровое поле, на котором размещаются все объекты (игрок, монеты). */
    @FXML
    protected Pane gamePane;

    /** Метка для отображения текущего счёта игрока. */
    @FXML
    protected Label scoreLabel;

    /**
     * Множество клавиш, которые сейчас зажаты пользователем.
     * Используется для плавного (непрерывного) перемещения игрока,
     * пока клавиша удерживается.
     */
    private Set<KeyCode> pressedKeys = new HashSet<>();

    /** Прямоугольник, представляющий игрока. */
    private Rectangle player;

    /** Текущее количество набранных очков. */
    private int score = 0;

    /** Список всех активных монет на поле. */
    private List<Circle> coins = new ArrayList<>();

    /** Размер стороны квадрата игрока (в пикселях). */
    private double PLAYER_SIZE = 30;

    /** Скорость перемещения игрока (пикселей за кадр). */
    private double SPEED = 3;

    /** Радиус монеты (в пикселях). */
    private double COIN_RADIUS = 10;

    /** Количество монет, одновременно находящихся на поле. */
    private int COINS_COUNT = 3;

    /** Цвет игрока (синий). */
    private Color BLUE = Color.rgb(0, 0, 255);

    private final double ENEMY_SIZE = 25;
    private double ENEMY_SPEED = 2;
    private long ENEMY_SPAWN_INTERVAL_NS = 1_500_000_000L;
    private long lastEnemySpawnNanos = 0;



    /** Генератор случайных чисел для позиционирования монет. */
    private Random random = new Random();

    private List<Enemy> enemies = new ArrayList<>();

    class Enemy {
        Rectangle rect;
        double dx;
        double dy;

        public Enemy(Rectangle rect, double dx, double dy) {
            this.rect = rect;
            this.dx = dx;
            this.dy = dy;
        }
    }

    /**
     * Инициализация контроллера. Вызывается JavaFX автоматически после загрузки FXML.
     * <p>
     * Создаёт игрока, настраивает обработчики клавиатуры, размещает монеты
     * и запускает игровой цикл.
     */
    @FXML
    public void initialize() {
        // Создаём игрока — синий квадрат с чёрной обводкой
        player = new Rectangle(PLAYER_SIZE, PLAYER_SIZE, BLUE);
        player.setX(320);
        player.setY(240);
        player.setStroke(Color.BLACK);
        player.setStrokeWidth(2);
        gamePane.getChildren().add(player);

        // Подписываемся на события клавиатуры сцены.
        // Platform.runLater нужен, потому что на момент initialize()
        // сцена ещё может быть не привязана к метке scoreLabel.
        Platform.runLater(() -> {
            scoreLabel.getScene().setOnKeyPressed(e -> {
                pressedKeys.add(e.getCode());
            });
            scoreLabel.getScene().setOnKeyReleased(e -> {
                pressedKeys.remove(e.getCode());
            });
        });

        spawnCoin(COINS_COUNT);

        // Запускаем игровой цикл: handle() вызывается на каждом кадре
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
            }
        }.start();
    }

    /**
     * Создаёт новую монету в случайной точке игрового поля
     * (внутри границ, с учётом радиуса) и добавляет её на панель.
     */
    private void spawnCoin(int coinsCount) {
        while (coinsCount > 0) {
            double paneWidth = gamePane.getPrefWidth();
            double paneHeight = gamePane.getPrefHeight();

            // Координаты центра монеты выбираются так, чтобы монета
            // целиком помещалась внутри панели
            double x = random.nextInt((int) (0 + COIN_RADIUS), (int) (paneWidth - COIN_RADIUS));
            double y = random.nextInt((int) (0 + COIN_RADIUS), (int) (paneHeight - COIN_RADIUS));

            Circle coin = new Circle(x, y, COIN_RADIUS, Color.GOLD);
            coin.setStroke(Color.ORANGE);
            coin.setStrokeWidth(1);

            coins.add(coin);
            gamePane.getChildren().add(coin);
            coinsCount--;
        }
    }

    private void spawnCoin() {
        double paneWidth = gamePane.getPrefWidth();
        double paneHeight = gamePane.getPrefHeight();

        // Координаты центра монеты выбираются так, чтобы монета
        // целиком помещалась внутри панели
        double x = random.nextInt((int) (0 + COIN_RADIUS), (int) (paneWidth - COIN_RADIUS));
        double y = random.nextInt((int) (0 + COIN_RADIUS), (int) (paneHeight - COIN_RADIUS));

        Circle coin = new Circle(x, y, COIN_RADIUS, Color.GOLD);
        coin.setStroke(Color.ORANGE);
        coin.setStrokeWidth(1);

        coins.add(coin);
        gamePane.getChildren().add(coin);
    }

    private void spawnEnemy() {
        double paneWidth = gamePane.getPrefWidth();
        double paneHeight = gamePane.getPrefHeight();

        int edge = random.nextInt(4);
        double x = 0, y = 0, dx = 0, dy = 0;
        if (edge == 0) {
            x = -ENEMY_SIZE;
            y = random.nextInt((int)(paneHeight - ENEMY_SIZE));
            dx = ENEMY_SPEED;
            dy = 0;
        }
        Rectangle rect = new Rectangle(ENEMY_SIZE, ENEMY_SIZE, Color.BLACK);
        rect.setX(x);
        rect.setY(y);
        enemies.add(new Enemy(rect, dx, dy));
        gamePane.getChildren().add(rect);
    }

    /**
     * Обновляет состояние игры на каждом кадре:
     * <ul>
     *     <li>перемещает игрока согласно зажатым клавишам WASD;</li>
     *     <li>удерживает игрока в пределах игрового поля;</li>
     *     <li>проверяет столкновения с монетами.</li>
     * </ul>
     */
    private void update(long now) {
        // Смещение игрока за текущий кадр
        double dx = 0, dy = 0;
        if (pressedKeys.contains(KeyCode.W)) {
            dy -= SPEED;
        }
        if (pressedKeys.contains(KeyCode.A)) {
            dx -= SPEED;
        }
        if (pressedKeys.contains(KeyCode.S)) {
            dy += SPEED;
        }
        if (pressedKeys.contains(KeyCode.D)) {
            dx += SPEED;
        }

        // Возможное новое положение игрока
        double nx = player.getX() + dx;
        double ny = player.getY() + dy;

        // Ограничение по левой/правой границе поля
        if (nx < 0) {
            nx = 0;
        }
        if (nx + PLAYER_SIZE > gamePane.getWidth()) {
            nx = gamePane.getWidth() - PLAYER_SIZE;
        }

        // Ограничение по верхней/нижней границе поля
        if (ny < 0) {
            ny = 0;
        }
        if (ny + PLAYER_SIZE > gamePane.getHeight()) {
            ny = gamePane.getHeight() - PLAYER_SIZE;
        }

        // Применяем новое положение
        player.setX(nx);
        player.setY(ny);

        List<Circle> takenCoins = new ArrayList<>();
        // Проверяем столкновения со всеми монетами
        for (Circle coin : coins) {
            if (intersects(player, coin)) {
                takenCoins.add(coin);
                score++;
                scoreLabel.setText("Счёт: " + score);
            }
        }

        for (Circle coin : takenCoins) {
            coins.remove(coin);
            gamePane.getChildren().remove(coin);
            spawnCoin();
        }

        if (lastEnemySpawnNanos == 0) {
            lastEnemySpawnNanos = now;
        }
        if (now - lastEnemySpawnNanos >= ENEMY_SPAWN_INTERVAL_NS) {
            spawnEnemy();
            lastEnemySpawnNanos = now;
        }

        updateEnemies();
        updatePlayerHitVisual();
    }

    private void updateEnemies() {
        double paneWidth = gamePane.getPrefWidth();
        double paneHeight = gamePane.getPrefHeight();

        List<Enemy> offScreenEnemies = new ArrayList<>();
        for (Enemy e : enemies) {
            e.rect.setX(e.rect.getX() + e.dx);
            e.rect.setY(e.rect.getY() + e.dy);

            boolean offScreen = e.rect.getX() > paneWidth + ENEMY_SIZE ||
                    e.rect.getX() < -ENEMY_SIZE ||
                    e.rect.getY() > paneHeight + ENEMY_SIZE ||
                    e.rect.getY() < -ENEMY_SIZE;

            if (offScreen) {
                offScreenEnemies.add(e);
            }
        }

        for (Enemy e : offScreenEnemies) {
            enemies.remove(e);
            gamePane.getChildren().remove(e.rect);
        }
    }

    private void updatePlayerHitVisual() {
        boolean hit = false;
        for (Enemy e : enemies) {
            if (e.rect.getBoundsInParent().intersects(player.getBoundsInParent())) {
                hit = true;
            }
        }
        if (hit) {
            player.setFill(Color.RED);
        } else {
            player.setFill(BLUE);
        }
//        player.setFill(hit ? Color.RED : BLUE);
    }

    /**
     * Проверяет, пересекаются ли прямоугольник и круг.
     * <p>
     * Метод находит ближайшую к центру круга точку прямоугольника
     * и сравнивает квадрат расстояния от неё до центра круга
     * с квадратом радиуса.
     *
     * @param rect   прямоугольник (игрок)
     * @param circle круг (монета)
     * @return {@code true}, если фигуры пересекаются; иначе {@code false}
     */
    private boolean intersects(Rectangle rect, Circle circle) {
        double circleX = circle.getCenterX();
        double circleY = circle.getCenterY();
        double radius = circle.getRadius();

        // Границы прямоугольника
        double left = rect.getX();
        double right = rect.getX() + rect.getWidth();
        double top = rect.getY();
        double bottom = rect.getY() + rect.getHeight();

        // Ближайшая к центру круга точка внутри прямоугольника
        double closestX = Math.max(left, Math.min(circleX, right));
        double closestY = Math.max(top, Math.min(circleY, bottom));

        // Квадрат расстояния от центра круга до этой точки
        double distX = closestX - circleX;
        double distY = closestY - circleY;
        return distX * distX + distY * distY <= radius * radius;
    }
}