package at.jku.se.smarthome.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Opens the system default browser to the application URL once the Spring Boot
 * application is fully started.
 *
 * <p>Only active when the {@code dist} profile is in use (i.e. when the
 * application is running as a packaged desktop installer, not during local
 * development).</p>
 */
@Component
@Profile("dist")
public class BrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);
    private static final String APP_URL = "http://localhost:8080";

    /**
     * Invoked after the application context is fully refreshed and the embedded
     * server is ready to accept requests. Opens a browser tab at
     * {@value #APP_URL}.
     *
     * @param event the application-ready event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", APP_URL);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", APP_URL);
            } else {
                pb = new ProcessBuilder("xdg-open", APP_URL);
            }
            pb.start();
        } catch (IOException e) {
            log.warn("Could not open browser automatically", e);
        }
    }
}
