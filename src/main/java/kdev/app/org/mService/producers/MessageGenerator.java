package kdev.app.org.mService.producers;

import kdev.app.org.mService.beans.Message;
import kdev.app.org.mService.config.AppConfig;
import kdev.app.org.mService.config.LoggingConfig;
import kdev.app.org.mService.receivers.MailHandler;
import kdev.app.org.mService.repository.MessageRepositoryImpl;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Map;

@Service
public class MessageGenerator {
    private final MessageRepositoryImpl repository;
    private final JmsTemplate jmsTemplate;
    private final LoggingConfig logger;
    private final AppConfig config;
    private final String tranTracker = "tranTracker.txt";
    private final String tranTracker103 = "tranTracker103.txt";
    private final String smsTracker = "smsTracker.txt";
    private final String loanTracker = "loanTracker.txt";
    private final String dueTracker = "dueTracker.txt";
    private final String arreasTracker = "arrearsTracker.txt";

    public MessageGenerator(MessageRepositoryImpl repository, MailHandler mailHandler, JmsTemplate jmsTemplate, LoggingConfig logger, AppConfig config) {
        this.repository = repository;
        this.jmsTemplate = jmsTemplate;
        this.logger = logger;
        this.config = config;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    private void sendTransactionalAlerts() {
        if ("Y".equalsIgnoreCase(config.getSendEmail())) {
            String[] templates = getTemplateFiles(".html");
            for (String template : templates) {
                if ("L".equalsIgnoreCase(template.substring(0, 1)) || "tTransaction103.html".equals(template))
                    continue;
                List<Map<String, Object>> trans = repository.getTransactionalMessages(tranTracker);
                String content = getTemplate(template);
                for (Map<String, Object> tran : trans) {
                    jmsTemplate.convertAndSend("sendMail",
                            Message.builder()
                                    .message(extractMsgFromTemplate(tran, content))
                                    .address(String.valueOf(tran.get("email_addr_1")))
                                    .id(Long.valueOf(tran.get("id").toString()))
                                    .isRetry(false).t_name(template)
                                    .subject("Loan Repayment Confirmation")
                                    .build());
                    logLastMessageId(tranTracker, String.valueOf(tran.get("id")));
                }
            }

            //newly added
            List<Map<String, Object>> trans = repository.getTransactionalMessages103(tranTracker103);
            String content = getTemplate("tTransaction103.html");
            for (Map<String, Object> tran : trans) {
                jmsTemplate.convertAndSend("sendMail",
                        Message.builder()
                                .message(extractMsgFromTemplate(tran, content))
                                .address(String.valueOf(tran.get("email_addr_1")))
                                .id(Long.valueOf(tran.get("id").toString()))
                                .isRetry(false).t_name("")
                                .subject("Loan Repayment Confirmation")
                                .build());
                logLastMessageId(tranTracker103, String.valueOf(tran.get("id")));
            }

        }
    }


    @Scheduled(cron = "*/5 * * ? * *")
    private void sendLoanAlerts() {
        if ("Y".equalsIgnoreCase(config.getSendEmail())) {

            String[] templates = getTemplateFiles(".html");
            for (String template : templates) {
                if ("T".equalsIgnoreCase(template.substring(0, 1)))
                    continue;
                String tracker = template.contains("Due") ? dueTracker : template.contains("Arrears") ? arreasTracker : loanTracker;
                List<Map<String, Object>> trans = template.contains("Due") ?
                        repository.getDueLoanMessages(tracker) : template.contains("Arrears") ?
                        repository.getLoanArrearsMessages(tracker) : repository.getLoanMessages(tracker);
                String content = getTemplate(template);
                for (Map<String, Object> tran : trans) {
                    jmsTemplate.convertAndSend("sendMail",
                            Message.builder()
                                    .message(extractMsgFromTemplate(tran, content))
                                    .address(String.valueOf(tran.get("email_addr_1")))
                                    .id(Long.valueOf(tran.get("id").toString()))
                                    .isRetry(false).t_name(template)
                                    .subject("Loan Repayment Reminder")
                                    .build());
                    logLastMessageId(tracker, String.valueOf(tran.get("id")));
                }
            }
        }
    }


    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    private void sendSms() {
        if ("Y".equalsIgnoreCase(config.getSendSms())) {
            String[] templates = getTemplateFiles(".txt");
            for (String template : templates) {
                List<Map<String, Object>> trans = repository.getTransactionalMessages(smsTracker);
                String content = getTemplate(template);
                for (Map<String, Object> tran : trans) {
                    jmsTemplate.convertAndSend("sendSMS", Message.builder().message(extractMsgFromTemplate(tran,
                            content)).address(String.valueOf(tran.get("phone_1"))).build());
                    logLastMessageId(smsTracker, String.valueOf(tran.get("id")));
                }
            }
        }
    }


    @Scheduled(fixedDelay = 50000, initialDelay = 50000)
    private void retryFailedMail() {
        if ("Y".equalsIgnoreCase(config.getRetrySending())) {
            List<Map<String, Object>> trans = repository.getFailedMessages();
            for (Map<String, Object> tran : trans) {
                String content = getTemplate(String.valueOf(tran.get("t_name")));
                jmsTemplate.convertAndSend("sendMail",
                        Message.builder()
                                .message(extractMsgFromTemplate(tran, content))
                                .address(String.valueOf(tran.get("email_addr_1")))
                                .id(Long.valueOf(tran.get("id").toString()))
                                .isRetry(true)
                                .t_name(String.valueOf(tran.get("t_name")))
                                .build());
                logLastMessageId(tranTracker, String.valueOf(tran.get("id")));
            }
        }
    }


    private String extractMsgFromTemplate(Map<String, Object> data, String msg) {
        try {
            for (Map.Entry<String, Object> entry : data.entrySet())
                msg = msg.replaceAll("\\{" + entry.getKey().trim() + "\\}", String.valueOf(entry.getValue()).trim());
        } catch (Exception e) {
            logger.loggingService().error(e.toString());
            return e.getLocalizedMessage();
        }
        return msg;
    }

    private String getTemplate(String filename) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader("templates/" + filename))) {
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private String[] getTemplateFiles(String fileType) {
        StringBuilder templates = new StringBuilder();
        File folder = new File("templates");
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(fileType)) {
                templates.append(listOfFiles[i].getName()).append(",");
            }
        }
        return templates.toString().split(",");
    }


    private void logLastMessageId(String filename, String id) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("tracker/" + filename))) {
            writer.write(id);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            logger.loggingService().error(e);
        }
    }

}
