package burp;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.ui.SnakeTab;

import javax.swing.SwingUtilities;

public class BurpExtender implements IBurpExtender {

    private static final String EXTENSION_NAME = "Snake 🐍 v1.0.0";

    private SnakeTab snakeTab;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        callbacks.setExtensionName(EXTENSION_NAME);

        // All Swing work must happen on the EDT
        SwingUtilities.invokeLater(() -> {
            snakeTab = new SnakeTab();
            callbacks.addSuiteTab(snakeTab);
        });

        // Register unload handler for clean teardown
        callbacks.registerExtensionStateListener(() -> {
            if (snakeTab != null) {
                snakeTab.dispose();
            }
        });
    }
}
