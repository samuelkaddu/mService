package kdev.app.org.mService.receivers;

import kdev.app.org.mService.beans.Message;
import kdev.app.org.mService.config.AppConfig;
import kdev.app.org.mService.config.LoggingConfig;
import kdev.app.org.mService.repository.MessageRepositoryImpl;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Component
public class MailHandler {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final AppConfig config;
    private final LoggingConfig logger;
    private final MessageRepositoryImpl repository;

    public MailHandler(JavaMailSender mailSender, MailProperties mailProperties, AppConfig config, LoggingConfig logger, MessageRepositoryImpl repository) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.config = config;
        this.logger = logger;
        this.repository = repository;
    }

    @JmsListener(destination = "sendMail", containerFactory = "busFactory")
    public void receiveMessage(Message emailMessage) {
        logger.loggingService().info(emailMessage);
        sendEmail(emailMessage);
    }


    public void sendEmail(Message content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress(mailProperties.getUsername()));
            message.addRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(content.getAddress()));
            message.setSubject("EQUI ALERTS");
            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content.getMessage(), "text/html");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            mailSender.send(message);
            if (content.isRetry())
                repository.removeRecoveredAlerts(content.getId());
            if ("Y".equalsIgnoreCase(config.getEnableLogging()))
                logger.loggingService().info("Sent email to {} ", content.getAddress());

        } catch (Exception ex) {
            logger.loggingService().error(ex);
            repository.logFailedAlerts(content.getId(), content.getT_name());
        }
    }


}
