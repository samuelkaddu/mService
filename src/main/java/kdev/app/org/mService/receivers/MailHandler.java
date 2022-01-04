package kdev.app.org.mService.receivers;

import kdev.app.org.mService.beans.Message;
import kdev.app.org.mService.config.AppConfig;
import kdev.app.org.mService.config.LoggingConfig;
import kdev.app.org.mService.repository.MessageRepositoryImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        sendEmail(emailMessage);
    }

    public void sendEmail(Message message) {
        String[] others = config.getCc_emails().split(",");
        String finalMessage;
        DataSource fds;
        try {
            finalMessage = modifyEmail(message.getMessage());
            MimeMessage mimemessage = mailSender.createMimeMessage();
            mimemessage.setFrom(new InternetAddress(mailProperties.getUsername()));
            for (String email : others) {
                if (!"".equalsIgnoreCase(email))
                    mimemessage.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(email));
            }
            mimemessage.addRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(message.getAddress()));
            mimemessage.setSubject(message.getSubject());
            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            messageBodyPart.setContent(finalMessage, "text/html");
            multipart.addBodyPart(messageBodyPart);

            if (message.getMessage() != null && message.getMessage().length() > 0) {
                for (int i = 0; i < getSrcTag(message.getMessage()).size(); i++) {
                    messageBodyPart = new MimeBodyPart();
                    fds = new FileDataSource(getSrcTag(message.getMessage()).get(i));
                    messageBodyPart.setDataHandler(new DataHandler(fds));
                    messageBodyPart.addHeader("Content-ID", "<image" + i + ">");
                    multipart.addBodyPart(messageBodyPart);
                }
            }
            mimemessage.setContent(multipart);
            mailSender.send(mimemessage);
            if (message.isRetry())
                repository.removeRecoveredAlerts(message.getId());
            if ("Y".equalsIgnoreCase(config.getEnableLogging()))
                logger.loggingService().info("Sent email to {} ", message.getAddress());

        } catch (Exception e) {
            logger.loggingService().error(e);
            repository.logFailedAlerts(message.getId(), message.getT_name());
        }
    }


    private String modifyEmail(String message) {
        int count = 0;
        Document doc = Jsoup.parseBodyFragment(message);
        Elements imgTags = doc.getElementsByTag("img");
        for (Element imgTag : imgTags) {
            imgTag.replaceWith(new Element(Tag.valueOf("img"), "").attr("src", "cid:image" + count).attr("style",
                    imgTag.attr("style")));
            count++;
        }
        return doc.outerHtml();
    }

    private ArrayList<String> getSrcTag(String html) {
        ArrayList<String> available = new ArrayList<>();
        Pattern p = Pattern.compile("<img[^>]*src=[\\\"']([^\\\"^']*)");
        Matcher m = p.matcher(html);
        while (m.find()) {
            String src = m.group();
            int startIndex = src.indexOf("src=") + 5;
            String srcTag = src.substring(startIndex);
            available.add(srcTag);
        }
        return available;
    }


}
