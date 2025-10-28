package com.example.darkchar;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.darkchar.ui.DarkCharacterGeneratorFxApp;

@SpringBootApplication
public class DarkCharacterGeneratorApplication {

    public static void main(String[] args) {
        Application.launch(DarkCharacterGeneratorFxApp.class, args);
    }
}
