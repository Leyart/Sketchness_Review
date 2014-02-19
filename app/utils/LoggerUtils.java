package utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import play.Logger;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class LoggerUtils {

    /*
     * Save the stack trace of an error to the log.
     * @param caller String representing the name of the module that has called the error
     * @param ex Exception raised by the module
     */
    public static void error(String caller, Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        Logger.error("[" + caller + "] ERROR: " + sw.toString());
    }
}
