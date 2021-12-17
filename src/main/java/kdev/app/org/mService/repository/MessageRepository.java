package kdev.app.org.mService.repository;

import java.util.List;
import java.util.Map;

public interface MessageRepository {
    List<Map<String, Object>> getTransactionalMessages(String filename);

    List<Map<String, Object>> getFailedMessages();

    void logFailedAlerts(Long id,String t_name);

    void removeRecoveredAlerts(Long id);
}
