//$Id$
package org.hibernate.test.annotations.reflection;

import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import org.hibernate.Logger;

/**
 * @author Emmanuel Bernard
 */
public class OtherLogListener {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, "Test Logger");

	@PrePersist
	@PostPersist
	public void log(Object entity) {
        LOG.debug("Logging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}


	public void noLog(Object entity) {
        LOG.debug("NoLogging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}
}
