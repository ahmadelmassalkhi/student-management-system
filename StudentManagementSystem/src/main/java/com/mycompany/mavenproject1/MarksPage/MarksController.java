/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mavenproject1.MarksPage;

// imports from same package
import com.itextpdf.text.DocumentException;
import com.mycompany.mavenproject1.Common.ComboBoxesOptions;
import com.mycompany.mavenproject1.ModelObjects.Student;
import com.mycompany.mavenproject1.models.StudentsModel;
import com.mycompany.mavenproject1.Common.StudentsTablePDFExporter;
import com.mycompany.mavenproject1.Exceptions.UserCancelledFileChooserException;
import com.mycompany.mavenproject1.ModelObjects.Subscription;

// imports from javafx
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

// other imports
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.io.FileNotFoundException;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 *
 * @author AHMAD
 */
public class MarksController implements Initializable {
    
    private StudentsModel model;

    @FXML
    private Label label_TotalSubscriptions;
    
    /*******************************************************************/

    /* TEXT FIELDS */
    @FXML
    private TextField tf_FullName;
    @FXML
    private TextField tf_Phone;
    @FXML
    private TextField tf_MinimumMark;
    @FXML
    private TextField tf_MaximumMark;
    
    /* COMBO BOXES */
    @FXML
    private ComboBox comboBox_Language;
    @FXML
    private ComboBox comboBox_Grade;
    @FXML
    private ComboBox comboBox_MarksOrder;

    /*******************************************************************/
    
    /* TABLE */
    @FXML
    private TableView studentsTable;
    
    /* COLUMNS */
    @FXML
    private TableColumn col_ID;
    @FXML
    private TableColumn col_FullName;
    @FXML
    private TableColumn col_Phone;
    @FXML
    private TableColumn col_Grade;
    @FXML
    private TableColumn col_Language;
    @FXML
    private TableColumn col_Mark;
    
    /*******************************************************************/

    /* CHECK BOXES */
    @FXML
    private CheckBox checkbox_FullName;
    @FXML
    private CheckBox checkbox_Phone;
    @FXML
    private CheckBox checkbox_Grade;
    @FXML
    private CheckBox checkbox_Language;

    /*******************************************************************/
    
    // so we are able to refresh data changes from outside the class object
    public MarksController() { marksController = this; }
    private static MarksController marksController = null;
    public static MarksController getController() { return marksController; }
    
    /*******************************************************************/
    
    private void initializeCheckBoxes() {
        
        // initially check all checkboxes
        checkbox_FullName.setSelected(true);
        checkbox_Phone.setSelected(true);
        checkbox_Grade.setSelected(true);
        checkbox_Language.setSelected(true);
        
        // add checkbox functionality to hide its corresponding column
        checkbox_FullName.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                col_FullName.setVisible(true);
            } else {
                col_FullName.setVisible(false);
            }
        });
        checkbox_Phone.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                col_Phone.setVisible(true);
            } else {
                col_Phone.setVisible(false);
            }
        });
        checkbox_Grade.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                col_Grade.setVisible(true);
            } else {
                col_Grade.setVisible(false);
            }
        });
        checkbox_Language.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                col_Language.setVisible(true);
            } else {
                col_Language.setVisible(false);
            }
        });
    }
    private void initializeTable() {
        // allow multiple selections
        studentsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // associate data to columns
        col_ID.setCellValueFactory(new PropertyValueFactory<Student, String>("id"));
        col_FullName.setCellValueFactory(new PropertyValueFactory<Student, String>("fullName"));
        col_Grade.setCellValueFactory(new PropertyValueFactory<Student, String>("grade"));
        col_Language.setCellValueFactory(new PropertyValueFactory<Student, String>("language"));
        col_Phone.setCellValueFactory(new PropertyValueFactory<Student, String>("phone"));
        col_Mark.setCellValueFactory(new PropertyValueFactory<Student, String>("mark"));
        
        // hide ID column by default (made to be interacted with, programmatically)
        col_ID.setVisible(false);
        
        // add functionality on row double clicks
        studentsTable.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                Student s = (Student) studentsTable.getSelectionModel().getSelectedItem();
                if (s != null) updateStudentMark(s);
            }
        });
        
        // restrict columns totalWidth = tableWidth
        ObservableList<TableColumn> allColumns = studentsTable.getColumns();
        for (TableColumn column : allColumns) {
            column.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                // get sum of all currently set widths (widths)
                double widthSum = 0;
                for(TableColumn col : allColumns) widthSum += col.getWidth();

                // if exceeds totalWidth, set the max width to the oldWidth
                if (widthSum > studentsTable.getWidth()) {
                    column.setMaxWidth(oldWidth.doubleValue()); // Revert back to oldWidth 
                                                                // (by setting maxWidth to oldWidth we enforce prefWidth = oldWidth without overlapping listener events resulting in errors)
                } else {
                    column.setMaxWidth(Double.MAX_VALUE); // reset max width
                }
            });
        }
    }
    private void initializeTextFields() {
        
        // set interactive filtering feature
        tf_FullName.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("[a-zA-Z]*")) {
                tf_FullName.setText(oldValue);
            } else Search();
        });
        tf_Phone.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            // force the field `Phone` to be numeric only
            if (!newValue.matches("\\d*")) {
                tf_Phone.setText(newValue.replaceAll("[^\\d]", ""));
            } else Search();
        });

        // force the field `Mark` to be numeric only (can contain up to 1 decimal point too)
        tf_MinimumMark.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                tf_MinimumMark.setText(oldValue); // Revert to the old value if not matching
            } else Search();
        });
        tf_MaximumMark.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                tf_MaximumMark.setText(oldValue); // Revert to the old value if not matching
            } else Search();
        });
    }
    private void initializeComboBoxes() {
                
        // Add items to the `Language` ComboBox
        comboBox_Language.setItems(FXCollections.observableArrayList(ComboBoxesOptions.OPTIONS_LANGUAGE));
        comboBox_Language.setValue(ComboBoxesOptions.OPTION_DEFAULT_LANGUAGE);

        // Add items to the `Grade` ComboBox
        comboBox_Grade.setItems(FXCollections.observableArrayList(ComboBoxesOptions.OPTIONS_GRADE));
        comboBox_Grade.setValue(ComboBoxesOptions.OPTION_DEFAULT_GRADE);
        
        // Add items to the `MarksOrder` ComboBox
        comboBox_MarksOrder.setItems(FXCollections.observableArrayList(ComboBoxesOptions.OPTIONS_MARKSORDER));
        comboBox_MarksOrder.setValue(ComboBoxesOptions.OPTION_DEFAULT_MARKORDER);
        
        // set interactive filtering feature
        comboBox_Language.valueProperty().addListener((obs, oldValue, newValue) -> {
            Search();
        });
        comboBox_Grade.valueProperty().addListener((obs, oldValue, newValue) -> {
            Search();
        });
        comboBox_MarksOrder.valueProperty().addListener((obs, oldValue, newValue) -> {
            Search();
        });
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.initializeCheckBoxes();
        this.initializeTextFields();
        this.initializeComboBoxes();
        this.initializeTable();
        
        // initialize model
        try {
            model = StudentsModel.getModel();
        } catch (IOException | SQLException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }

        this.refresh();
    }
    
    private void updateStudentMark(Student s) {
        try {
            // load resource
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateStudentMark.fxml"));
            Parent root = loader.load();

            // create window
            Stage stage = new Stage();
            stage.setScene(new Scene(root, root.prefWidth(-1), root.prefHeight(-1)));
            
            // Center stage on screen
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((screenBounds.getWidth() - root.prefWidth(-1)) / 2);
            stage.setY((screenBounds.getHeight() - root.prefHeight(-1)) / 2);
            
            // Access the controller and set the data
            UpdateStudentMarkController controller = loader.getController();
            controller.setWindowInformation(stage, root);
            controller.setStudent(s);
            
            // wait until student is successfully updated or cancelled
            stage.showAndWait();
            
            // adopt to data changes
            this.refresh();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
        
    /*******************************************************************/
    
    /*
     * refresh data (stats, and table based on inputs)
     * public because it is used in `Controller` to make sure data is up to date with possible changes to the database, made by other pages
     */
    public void refresh() {
        this.setStats();
        this.Search();
    }
    
    private void setStats() {
        try {
            label_TotalSubscriptions.setText(model.getNumberOfSubscriptions() + "");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
    
    private void Search() {
        // extract data from input-fields
        String fullName = tf_FullName.getText();
        String phone = tf_Phone.getText();
        String grade =  (String) comboBox_Grade.getValue();
        String language = (String) comboBox_Language.getValue();
        String MarksOrder = (String) comboBox_MarksOrder.getValue();
        
        // set to active (by convension, Marks page should contain students with active subscriptions)
        Subscription subscription = new Subscription();
        subscription.setStatus(Boolean.TRUE);
        
        // fix input-data format
        if(fullName.isEmpty()) fullName = null;
        if(phone.isEmpty()) phone = null;
        if(grade.isEmpty() || grade.equals("Any")) grade = null;
        if(language.isEmpty() || language.equals("Any")) language = null;
        
        // the new letter added might be a `.` => `19.` is not a number !!
        Float MinimumMark, MaximumMark;
        try {
            MinimumMark = (tf_MinimumMark.getText().isEmpty()) ? null : Float.valueOf(tf_MinimumMark.getText());
            MaximumMark = (tf_MaximumMark.getText().isEmpty()) ? null : Float.valueOf(tf_MaximumMark.getText());
            
            // get & display filtered students
            List<Student> result = model.Read(
                    null, 
                    fullName, 
                    phone, 
                    grade, 
                    language, 
                    subscription, 
                    MinimumMark, 
                    MaximumMark, 
                    MarksOrder
            );
            studentsTable.setItems(FXCollections.observableArrayList(result));
        } catch (NumberFormatException ex) {
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
    
    /*******************************************************************/
    
    public void export() {
        try {
            StudentsTablePDFExporter exporter = new StudentsTablePDFExporter(studentsTable);
            
            // set stage (used to prevent interaction with it during FileChooser)
            exporter.setStage((Stage) studentsTable.getScene().getWindow());
            
            // set target columns
            exporter.setColFullName(col_FullName);
            exporter.setColPhone(col_Phone);
            exporter.setColGrade(col_Grade);
            exporter.setColLanguage(col_Language);
            exporter.setColMark(col_Mark);
            
            // export
            exporter.Export();
        } catch (UserCancelledFileChooserException ex) {
          // do nothing
        } catch (DocumentException | FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
    
    /*******************************************************************/
}
