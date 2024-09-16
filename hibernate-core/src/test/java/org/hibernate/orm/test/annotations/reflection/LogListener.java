/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.reflection;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;

import org.jboss.logging.Logger;


/**
 * @author Emmanuel Bernard
 */
public class LogListener {
	private static final Logger log = Logger.getLogger( LogListener.class );

	@PrePersist
	@PostPersist
	public void log(Object entity) {
		log.debug("Logging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}

	public void noLog(Object entity) {
		log.debug("NoLogging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}
}
