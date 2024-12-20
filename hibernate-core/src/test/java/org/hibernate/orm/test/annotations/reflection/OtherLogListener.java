/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.reflection;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class OtherLogListener {
	private static final Logger log = Logger.getLogger( OtherLogListener.class );

	@PrePersist
	@PostPersist
	public void log(Object entity) {
		log.debug("Logging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}

	public void noLog(Object entity) {
		log.debug("NoLogging entity " + entity.getClass().getName() + " with hashCode: " + entity.hashCode());
	}
}
