package com.example.darkchar.ui.controller;

import com.example.darkchar.domain.GeneratedCharacter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class CharacterResultController {

    @FXML
    private TextArea resultTextArea;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setGeneratedCharacter(GeneratedCharacter generatedCharacter) {
        if (generatedCharacter == null) {
            resultTextArea.clear();
        } else {
            resultTextArea.setText(generatedCharacter.narrative());
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        if (stage != null) {
            stage.close();
        }
    }
}
