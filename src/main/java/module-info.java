module ru.otus.curs.remotedesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens ru.otus.curs.remotedesktop to javafx.fxml;
    exports ru.otus.curs.remotedesktop;
}