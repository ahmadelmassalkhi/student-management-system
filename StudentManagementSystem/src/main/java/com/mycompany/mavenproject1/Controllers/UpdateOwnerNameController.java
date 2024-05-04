/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mavenproject1.Controllers;

// imports from same project
import com.mycompany.mavenproject1.Common.ErrorAlert;
import com.mycompany.mavenproject1.Managers.ConfigurationManager;

// imports from javafx
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// other imports
import java.io.IOException;

/**
 *
 * @author AHMAD
 */
public class UpdateOwnerNameController {
    
    /*******************************************************************/
    /* WINDOW PROPERTIES INITIALIZATION METHODS */
    
    // DRAGGABLE WINDOW FEATURE
    private Stage stage;
    private double x, y;
    public void setWindowInformation(Stage stage, Parent root) {
        this.stage = stage;
        
        // block interaction with the parent window until it's closed
        stage.initModality(Modality.APPLICATION_MODAL);
        
        // set stage borderless
        stage.initStyle(StageStyle.UNDECORATED);

        // make it draggable
        root.setOnMousePressed(event -> {
            this.x = event.getSceneX();
            this.y = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - this.x);
            stage.setY(event.getScreenY() - this.y);
        });
    }
    
    @FXML
    private TextField tf_OwnerName;
    public void setOwnerName(String newName) {
        this.tf_OwnerName.setText(newName);
    }
    
    /*******************************************************************/
    
    public void Cancel() {
        stage.close();
    }
    
    public void Update() {
        
        String newName = this.tf_OwnerName.getText();
        if(newName.isEmpty()) {
            ErrorAlert alert = new ErrorAlert("Error", "Invalid Input !", "Please enter a Name !");
            alert.showAndWait();
            return;
        }
        
        try {
            ConfigurationManager.getManager().updateOwnerName(newName);
            Cancel();
        } catch (IOException | IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
    
    /*******************************************************************/
    
    
}
