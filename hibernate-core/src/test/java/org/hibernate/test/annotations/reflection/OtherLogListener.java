//$Id$
package org.hibernate.test.annotations.reflection;
import static org.hibernate.testing.TestLogger.LOG;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
public class OtherLogListener {

	@PrePersist
	@PostPersist
	public void log(Object entity) {
        LOG.debug("Logging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}


	public void noLog(Object entity) {
        LOG.debug("NoLogging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}
}
