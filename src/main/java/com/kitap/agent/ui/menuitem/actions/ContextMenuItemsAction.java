package com.kitap.agent.ui.menuitem.actions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * @Author: KT1497
 *
 * @Description: Actions performed when clicked on contextMenu Items of TrayIcon
 */
@Slf4j
@Component
public class ContextMenuItemsAction {

//    @Autowired
//    CallDeregisterApi callDeregisterApi;

    /**
     * @Description Action performed when register menuitem from context menu is clicked
     */
    public void registerMenuItemAction(Stage stage) {
        URL xmlUrl = getClass().getResource("/menuItems/registerMenu.fxml");
        FXMLLoader loader = new FXMLLoader(xmlUrl);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            log.error(ex.toString());
            throw new RuntimeException(ex);
        }
        Scene scene = new Scene(root);
        stage.setTitle("Registration");
        javafx.scene.image.Image icon = new javafx.scene.image.Image("images/KairosIcon.png");
        stage.getIcons().add((icon));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * @Description Action performed when Generate/Execute menuitem from context menu is clicked;
     * Displaying different UI in single Pane using JavaFX TabPane
     */
    public void genExeMenuItemAction(Stage stage) {
        URL xmlUrl = getClass().getResource("/menuItems/genOrExe.fxml");
        FXMLLoader loader = new FXMLLoader(xmlUrl);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            log.error(ex.toString());
            throw new RuntimeException(ex);
        }
        Scene scene = new Scene(root);
        stage.setTitle("Generate/Execute Page");
        javafx.scene.image.Image icon = new javafx.scene.image.Image("images/KairosIcon.png");
        stage.getIcons().add((icon));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * @Description Action performed when deRegister menuitem from context menu is clicked
     */
    public void deregisterMenuItemAction() {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Deregistration");
        confirmationAlert.setHeaderText("Confirmation Alert");
        confirmationAlert.setContentText("Are you sure ??");
        confirmationAlert.showAndWait().ifPresent((btnType) -> {
            if (btnType == ButtonType.OK) {
                //callDeregisterApi.deRegisterAgent();
                Alert informationAlert = new Alert(Alert.AlertType.INFORMATION);
                informationAlert.setTitle("Deregistration");
                informationAlert.setContentText("Agent is Deregistered");
                informationAlert.showAndWait();
                log.info("Agent is Deregistered");
            } else if (btnType == ButtonType.CANCEL) {
                log.info("Agent is NOT Deregistered");
            }
        });
    }

    /**
     * @Description Action performed when execute menuitem from context menu is clicked
     */
    public void executeMenuItemAction(Stage stage) {
        URL xmlUrl = getClass().getResource("/menuItems/executeMenu.fxml");
        FXMLLoader loader = new FXMLLoader(xmlUrl);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            log.error(ex.toString());
            throw new RuntimeException(ex);
        }
        Scene scene = new Scene(root);
        stage.setTitle("Execute Tests");
        javafx.scene.image.Image icon = new javafx.scene.image.Image("images/KairosIcon.png");
        stage.getIcons().add((icon));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * @Description Action performed when generate menuitem from context menu is clicked
     */
    public void generateMenuItemAction(Stage stage) {

/* ConfigurableApplicationContext applicationContext = null;
        FxWeaver fxWeaver = applicationContext.getBean(FxWeaver.class);
        Parent root = fxWeaver.loadView(GenerateMenu.class);*//*
         */
/* FXMLLoader loader = new FXMLLoader(getClass().getResource("/menuItems/generateMenu.fxml"));

        ConfigurableApplicationContext context = null;
        loader.setControllerFactory(context::getBean);

        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        URL xmlUrl = getClass().getResource("/menuItems/generateMenu.fxml");
        FXMLLoader loader = new FXMLLoader(xmlUrl);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            log.error(ex.toString());
            throw new RuntimeException(ex);
        }
        Scene scene = new Scene(root);
        stage.setTitle("Generate Tests");
        javafx.scene.image.Image icon = new javafx.scene.image.Image("images/KairosIcon.png");
        stage.getIcons().add((icon));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
