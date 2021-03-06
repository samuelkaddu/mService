package kdev.app.org.mService.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "app.service")
@Configuration("AppConfig")
public class AppConfig {
    private String coreSchema;
    private String enableLogging;
    private String tranQuery;
    private String tranQuery103;
    private String loanQuery;
    private String dueQuery;
    private String arrearsQuery;
    private String sendSms;
    private String sendEmail;
    private String retrySending;
    private String cc_emails;


}