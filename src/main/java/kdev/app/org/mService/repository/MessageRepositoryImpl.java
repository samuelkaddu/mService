package kdev.app.org.mService.repository;

import kdev.app.org.mService.config.AppConfig;
import kdev.app.org.mService.config.LoggingConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AppConfig config;
    private final LoggingConfig logger;

    public MessageRepositoryImpl(JdbcTemplate jdbcTemplate, AppConfig config, LoggingConfig logger) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public List<Map<String, Object>> getTransactionalMessages(String filename) {
        try {
            Long lastMessageId = getLastMessageId(filename);
            String query = new StringBuilder("select * from ")
                    .append(config.getTranQuery())
                    .append(" where id > ")
                    .append(lastMessageId)
                    .append(" order by id asc").toString();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            return results;
        } catch (Exception e) {
            logger.loggingService().error(e);
            return Arrays.asList();
        }
    }
    @Override
    public List<Map<String, Object>> getTransactionalMessages103(String filename) {
        try {
            Long lastMessageId = getLastMessageId(filename);
            String query = new StringBuilder("select * from ")
                    .append(config.getTranQuery103())
                    .append(" where id > ")
                    .append(lastMessageId)
                    .append(" order by id asc").toString();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            return results;
        } catch (Exception e) {
            logger.loggingService().error(e);
            return Arrays.asList();
        }
    }


    @Override
    public List<Map<String, Object>> getLoanMessages(String filename) {
        try {
            Long lastMessageId = getLastMessageId(filename);
            String query = new StringBuilder("select * from ")
                    .append(config.getLoanQuery())
                    .append(" where id > ")
                    .append(lastMessageId)
                    .append(" order by id asc").toString();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            return results;
        } catch (Exception e) {
            logger.loggingService().error(e);
            return Arrays.asList();
        }
    }


    public List<Map<String, Object>> getDueLoanMessages(String filename) {
        try {
            Long lastMessageId = getLastMessageId(filename);
            String query = new StringBuilder("select * from ")
                    .append(config.getDueQuery())
                    .append(" where id > ")
                    .append(lastMessageId)
                    .append(" order by id asc").toString();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            return results;
        } catch (Exception e) {
            logger.loggingService().error(e);
            return Arrays.asList();
        }
    }

    @Override
    public List<Map<String, Object>> getLoanArrearsMessages(String filename) {
        try {
            Long lastMessageId = getLastMessageId(filename);
            String query = new StringBuilder("select * from ")
                    .append(config.getArrearsQuery())
                    .append(" where id > ")
                    .append(lastMessageId)
                    .append(" order by id asc").toString();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            return results;
        } catch (Exception e) {
            logger.loggingService().error(e);
            return Arrays.asList();
        }
    }

    @Override
    public List<Map<String, Object>> getFailedMessages() {
        List<Map<String, Object>> results = jdbcTemplate.queryForList("select * from v_failed_messages");
        return results;
    }

    @Override
    public void logFailedAlerts(Long id, String t_name) {
        jdbcTemplate.update("insert into failed_alerts(id,t_name) values (?,?)", id, t_name);
    }

    @Override
    public void removeRecoveredAlerts(Long id) {
        jdbcTemplate.update("delete from  failed_alerts where id = ?", id);
    }

    private Long getLastMessageId(String filename) {
        String id = "0";
        try (BufferedReader in = new BufferedReader(new FileReader("tracker/" + filename))) {
            String str;
            while ((str = in.readLine()) != null) {
                id = str;
            }
            return Long.valueOf(id);
        } catch (IOException e) {
            if (!e.getMessage().contains("The system cannot find the file specified"))
                logger.loggingService().error(e);
            return 0L;
        }
    }


}
