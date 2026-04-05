package hotel;

/**
 * Entry point wrapper for JavaFX.
 * 
 * Why does this exist?
 * In modern Java (11+), if your main class extends javafx.application.Application,
 * the Java launcher strictly expects the project to be fully modular (using module-info.java).
 * By keeping this separate App class that does NOT extend Application, we bypass that strict
 * check, allowing the project to run seamlessly via VS Code's "Run" button without complex module configuration.
 */
public class App {
    public static void main(String[] args) {
        Main.main(args);
    }
}
