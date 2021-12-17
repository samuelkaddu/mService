package kdev.app.org.mService.receivers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;
import ug.ac.mak.java.logger.Log;

@Service
public class JMSErrorHandler implements ErrorHandler {

    @Autowired
    private Log logHandler;

    @Override
    public void handleError(Throwable t) {
        logHandler.error(t);
    }
}
