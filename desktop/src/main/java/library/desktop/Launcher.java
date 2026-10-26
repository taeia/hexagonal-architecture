package library.desktop;

/**
 * Starts the application from an IDE.
 *
 * JavaFX is a plain Maven dependency here, so it sits on the classpath rather than
 * the module path. When the main class itself extends {@code Application}, the Java
 * launcher insists on finding the JavaFX modules and stops with "JavaFX runtime
 * components are missing". This class does not extend {@code Application}, so that
 * check is skipped and {@code MainWindow} starts normally.
 *
 * {@code mvn javafx:run} does not need this class; it sets up the module path itself.
 */
public class Launcher {

    public static void main(String[] args) {
        MainWindow.main(args);
    }
}
