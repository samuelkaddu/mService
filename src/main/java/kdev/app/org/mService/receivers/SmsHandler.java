package kdev.app.org.mService.receivers;

import kdev.app.org.mService.beans.Message;
import kdev.app.org.mService.config.AppConfig;
import kdev.app.org.mService.config.LoggingConfig;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class SmsHandler {

    private final AppConfig config;
    private final LoggingConfig logger;

    public SmsHandler(AppConfig config, LoggingConfig logger) {
        this.config = config;
        this.logger = logger;
    }


    @JmsListener(destination = "sendSMS", containerFactory = "busFactory")
    public void receiveMessage(Message message) {
        logger.loggingService().info(message);
        //sendEmail(emailMessage);
    }

}
