package org.example.lesson1210;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashSet;
import java.util.Set;

public class GameController {

    @FXML
    protected Pane gamePane;

    @FXML
    protected Label scoreLabel;

    private Set<KeyCode> pressedKeys = new HashSet<>();

    private Rectangle player;

    private double PLAYER_SIZE = 30;
    private double SPEED = 3;

    @FXML
    public void initialize() {

        player = new Rectangle(PLAYER_SIZE, PLAYER_SIZE, Color.LIME);
        player.setX(320);
        player.setY(240);
        player.setStroke(Color.BLACK);
        player.setStrokeWidth(2);
        gamePane.getChildren().add(player);

        Platform.runLater(() -> {
            scoreLabel.getScene().setOnKeyPressed(e -> {
                pressedKeys.add(e.getCode());
            });
            scoreLabel.getScene().setOnKeyReleased(e -> {
                pressedKeys.remove(e.getCode());
            });
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        }.start();
    }

    private void update(){
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
        double nx = player.getX() + dx; // Возможное новое положение
        double ny = player.getY() + dy;
        if (nx < 0) {
            nx = 0;
        }
        if (nx + PLAYER_SIZE > gamePane.getWidth()) {
            nx = gamePane.getWidth() - PLAYER_SIZE;
        }
        if (ny < 0) {
            ny = 0;
        }
        if (ny + PLAYER_SIZE > gamePane.getHeight()) {
            ny = gamePane.getHeight() - PLAYER_SIZE;
        }

        player.setX(nx);
        player.setY(ny);
    }
}