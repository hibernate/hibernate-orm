//$Id$
package org.hibernate.test.annotations.reflection;

import javax.persistence.PrePersist;
import javax.persistence.PostPersist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Emmanuel Bernard
 */
public class LogListener {
	private final Logger log = LoggerFactory.getLogger( LogListener.class );

	@PrePersist
	@PostPersist
	public void log(Object entity) {
		log.debug( "Logging entity {} with hashCode: {}", entity.getClass().getName(), entity.hashCode() );
	}


	public void noLog(Object entity) {
		log.debug( "NoLogging entity {} with hashCode: {}", entity.getClass().getName(), entity.hashCode() );
	}
}
