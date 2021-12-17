package kdev.app.org.mService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ug.ac.mak.java.logger.DailyLogListener;
import ug.ac.mak.java.logger.Log;
import ug.ac.mak.java.logger.Logger;
import ug.ac.mak.java.logger.SimpleLogListener;

import java.io.File;

@Component
public class LoggingConfig {

    @Bean
    public Log loggingService() {
        new File("logs/").mkdirs();
        Logger logger = new Logger();
        logger.addListener(new SimpleLogListener());
        DailyLogListener dailyLogger = new DailyLogListener();
        dailyLogger.setConfiguration("logs/events", "gzip");
        logger.addListener(dailyLogger);
        return new Log(logger, "mService");
    }
}
