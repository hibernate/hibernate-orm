package org.hibernate.spatial;

import org.jboss.logging.Logger;

/**
 * A static factory for <code>Log</code>s.
 *
 * <p>This class is based on hibernate-ogm LoggerFactory class.</p>
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/14/12
 */
public class LogFactory {

    public static Log make() {
        Throwable t = new Throwable();
        StackTraceElement directCaller = t.getStackTrace()[1];
        return Logger.getMessageLogger(Log.class, directCaller.getClassName());
    }


}
