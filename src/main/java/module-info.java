module org.example.lesson1210 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens org.example.lesson1210 to javafx.fxml;
    exports org.example.lesson1210;
}