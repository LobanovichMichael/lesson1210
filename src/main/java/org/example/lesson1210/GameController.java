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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** Метка для отображения лучшего результата (рекорда). */
    @FXML
    protected Label bestScoreLabel;

    /** Метка для отображения оставшегося количества жизней игрока. */
    @FXML
    protected Label hitPointsLabel;

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

    /** Лучший результат за все игры, сохранённый в файле. */
    private int bestScore = 0;

    /** Текущее количество жизней игрока. */
    private int playerHP = 3;

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

    /** Размер стороны квадрата врага (в пикселях). */
    private final double ENEMY_SIZE = 25;

    /** Скорость перемещения врага (пикселей за кадр). */
    private double ENEMY_SPEED = 2;

    /** Интервал между появлениями врагов в наносекундах (1.5 секунды). */
    private long ENEMY_SPAWN_INTERVAL_NS = 1_500_000_000L;

    /** Длительность состояния получения игроком урона в наносекундах. */
    private long PLAYER_HIT_DURATION_NS = 1_500_000_000L;

    /** Время последнего появления врага в наносекундах; 0 — враг ещё не создавался. */
    private long lastEnemySpawnNanos = 0;

    /** Время последнего получения игроком урона в наносекундах. */
    private long lastPlayerHitNanos = 0;

    /** Генератор случайных чисел для позиционирования монет и врагов. */
    private Random random = new Random();

    /** Список активных врагов на игровом поле. */
    private List<Enemy> enemies = new ArrayList<>();

    /**
     * Внутренний класс, описывающий врага.
     * Хранит прямоугольник для отрисовки и вектор движения.
     */
    class Enemy {
        /** Прямоугольник, отображающий врага на игровом поле. */
        Rectangle rect;

        /** Смещение врага по оси X за один кадр. */
        double dx;

        /** Смещение врага по оси Y за один кадр. */
        double dy;

        /**
         * Создаёт нового врага.
         *
         * @param rect прямоугольник врага
         * @param dx   смещение по X за кадр
         * @param dy   смещение по Y за кадр
         */
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
        bestScore = readBestScore();
        bestScoreLabel.setText("Рекорд: " + bestScore);
        hitPointsLabel.setText("Жизни: " + playerHP);

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

        // Размещаем стартовое количество монет на поле
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
     * Создаёт несколько монет в случайных точках игрового поля.
     *
     * @param coinsCount количество создаваемых монет
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

    /**
     * Создаёт одну новую монету в случайной точке игрового поля
     * и добавляет её на панель.
     */
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

    /**
     * Создаёт нового врага у края игрового поля и задаёт ему направление движения.
     * <p>
     * Враг может появиться у любого из четырёх краёв поля и двигаться
     * по направлению внутрь игровой области.
     */
    private void spawnEnemy() {
        double paneWidth = gamePane.getPrefWidth();
        double paneHeight = gamePane.getPrefHeight();

        // Определяем край появления врага.
        // 0 — левый, 1 — верхний, 2 — правый, 3 — нижний.
        int edge = random.nextInt(4);
        double x, y, dx, dy;
        if (edge == 0) {
            x = -ENEMY_SIZE;
            y = random.nextInt((int) (paneHeight - ENEMY_SIZE));
            dx = ENEMY_SPEED;
            dy = 0;
        } else if (edge == 1) {
            x = random.nextInt((int) (paneWidth - ENEMY_SIZE));
            y = -ENEMY_SIZE;
            dx = 0;
            dy = ENEMY_SPEED;
        } else if (edge == 2) {
            x = paneWidth + ENEMY_SIZE;
            y = random.nextInt((int) (paneHeight - ENEMY_SIZE));
            dx = -ENEMY_SPEED;
            dy = 0;
        } else {
            x = random.nextInt((int) (paneWidth - ENEMY_SIZE));
            y = paneHeight + ENEMY_SIZE;
            dx = 0;
            dy = -ENEMY_SPEED;
        }

        // Создаём визуальное представление врага и задаём начальную позицию
        Rectangle rect = new Rectangle(ENEMY_SIZE, ENEMY_SIZE, Color.BLACK);
        rect.setX(x);
        rect.setY(y);

        // Добавляем врага в список активных врагов и на игровое поле
        enemies.add(new Enemy(rect, dx, dy));
        gamePane.getChildren().add(rect);
    }

    /**
     * Обновляет состояние игры на каждом кадре:
     * <ul>
     *     <li>перемещает игрока согласно зажатым клавишам WASD;</li>
     *     <li>удерживает игрока в пределах игрового поля;</li>
     *     <li>проверяет столкновения с монетами и начисляет очки;</li>
     *     <li>создаёт новых врагов по таймеру и перемещает существующих;</li>
     *     <li>обновляет визуальное состояние игрока при получении урона.</li>
     * </ul>
     *
     * @param now текущее время кадра в наносекундах
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
                if (score > bestScore) {
                    bestScore = score;
                    bestScoreLabel.setText("Рекорд: " + bestScore);
                    writeBestScore(bestScore);
                }
            }
        }

        // Удаляем собранные монеты и создаём вместо них новые
        for (Circle coin : takenCoins) {
            coins.remove(coin);
            gamePane.getChildren().remove(coin);
            spawnCoin();
        }

        // Инициализируем время последнего спавна врага при первом кадре
        if (lastEnemySpawnNanos == 0) {
            lastEnemySpawnNanos = now;
        }

        // Проверяем, пора ли создать нового врага
        if (now - lastEnemySpawnNanos >= ENEMY_SPAWN_INTERVAL_NS) {
            spawnEnemy();
            lastEnemySpawnNanos = now;
        }

        updateEnemies();
        updatePlayerHitVisual(now);
    }

    /**
     * Перемещает всех врагов и удаляет тех, кто вышел за пределы игрового поля.
     */
    private void updateEnemies() {
        double paneWidth = gamePane.getPrefWidth();
        double paneHeight = gamePane.getPrefHeight();

        List<Enemy> offScreenEnemies = new ArrayList<>();
        for (Enemy e : enemies) {
            // Сдвигаем врага согласно его вектору движения
            e.rect.setX(e.rect.getX() + e.dx);
            e.rect.setY(e.rect.getY() + e.dy);

            // Проверяем, вышел ли враг за границы поля
            boolean offScreen = e.rect.getX() > paneWidth + ENEMY_SIZE ||
                    e.rect.getX() < -ENEMY_SIZE ||
                    e.rect.getY() > paneHeight + ENEMY_SIZE ||
                    e.rect.getY() < -ENEMY_SIZE;

            if (offScreen) {
                offScreenEnemies.add(e);
            }
        }

        // Удаляем врагов, находящихся за пределами поля
        for (Enemy e : offScreenEnemies) {
            enemies.remove(e);
            gamePane.getChildren().remove(e.rect);
        }
    }

    /**
     * Обновляет состояние игрока при столкновении с врагом:
     * уменьшает количество жизней с учётом задержки между попаданиями
     * и временно окрашивает игрока в красный цвет.
     *
     * @param now текущее время кадра в наносекундах
     */
    /**
     * Обновляет состояние игрока при столкновении с врагом:
     * уменьшает количество жизней с учётом задержки между попаданиями
     * и временно окрашивает игрока в красный цвет.
     *
     * @param now текущее время кадра в наносекундах
     */
    private void updatePlayerHitVisual(long now) {
        // Перебираем всех активных врагов на поле.
        for (Enemy e : enemies) {
            // Проверяем, пересекаются ли прямоугольники врага и игрока.
            // getBoundsInParent() возвращает границы фигуры в системе координат родителя,
            // а intersects(...) проверяет пересечение этих прямоугольных областей.
            if (e.rect.getBoundsInParent().intersects(player.getBoundsInParent())) {

                // Урон наносится не каждый кадр, а только если с момента последнего
                // попадания прошло больше PLAYER_HIT_DURATION_NS наносекунд.
                // Это своеобразная "неуязвимость" после получения урона:
                // пока игрок стоит на враге, жизни не будут уменьшаться каждый кадр.
                if (now > lastPlayerHitNanos + PLAYER_HIT_DURATION_NS) {
                    // Уменьшаем количество жизней игрока на 1.
                    playerHP -= 1;

                    // Отладочный вывод текущего количества жизней в консоль.
                    System.out.println(playerHP);

                    // Запоминаем время текущего попадания.
                    // Именно от этого значения будет отсчитываться следующий период неуязвимости.
                    lastPlayerHitNanos = now;

                    // Обновляем текстовую метку с количеством жизней на экране.
                    hitPointsLabel.setText("Жизни: " + playerHP);
                }
            }
        }

        // Если с момента последнего попадания прошло меньше PLAYER_HIT_DURATION_NS,
        // то игрок ещё находится в состоянии "получил урон" — окрашиваем его в красный.
        // Это визуальная индикация того, что игрок недавно столкнулся с врагом.
        if (lastPlayerHitNanos + PLAYER_HIT_DURATION_NS > now) {
            player.setFill(Color.RED);
        } else {
            // Иначе возвращаем игроку его обычный синий цвет.
            player.setFill(BLUE);
        }
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

    /**
     * Читает лучший результат из файла.
     * <p>
     * Если файл отсутствует или не может быть прочитан, возвращается 0.
     * Если содержимое файла повреждено, файл перезаписывается значением 0.
     *
     * @return лучший результат, сохранённый ранее, или 0 при ошибке чтения
     */
    private int readBestScore() {
        Path filePath = Path.of("src/main/resources/org/example/lesson1210/bestScore.txt");
        int result = 0;

        try {
            result = Integer.parseInt(Files.readString(filePath));
        } catch (IOException e) {
            System.out.println("Не удалось прочитать лучший результат предыдущих игр!");
        } catch (NumberFormatException e) {
            System.out.println("Файл с рекордом был повреждён");
            try {
                Files.writeString(filePath, "0");
            } catch (IOException e1) {
                System.out.println("Не удалось исправить файл с рекордом");
            }
        }

//        try (Scanner sc = new Scanner(filePath)) {
//            result = sc.nextInt();
//        } catch (IOException e) {
//            System.out.println("Не удалось прочитать лучший результат предыдущих игр!");
//        }

        return result;
    }

    /**
     * Записывает лучший результат в файл.
     *
     * @param score новый лучший результат, который нужно сохранить
     */
    private void writeBestScore(int score) {
        Path path = Path.of("src/main/resources/org/example/lesson1210/bestScore.txt");

        try {
            Files.writeString(path, String.valueOf(score));
        } catch (IOException e) {
            System.out.println("Не удалось записать рекорд в файл");
        }
    }
}