module com.mycompany.mavenproject1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.base;

    opens com.mycompany.mavenproject1.Controllers to javafx.fxml;
    opens com.mycompany.mavenproject1 to javafx.fxml;
    opens com.mycompany.mavenproject1.models to javafx.base;
    opens com.mycompany.mavenproject1.ModelObjects to javafx.base;
    exports com.mycompany.mavenproject1;
    requires itextpdf;
}