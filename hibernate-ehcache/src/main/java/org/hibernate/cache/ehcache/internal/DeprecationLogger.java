/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.hibernate.internal.log.DeprecationLogger.CATEGORY;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 20100, max = 20100)
public interface DeprecationLogger extends BasicLogger {
	/**
	 * Singleton access
	 */
	DeprecationLogger INSTANCE = Logger.getMessageLogger(
			DeprecationLogger.class,
			CATEGORY
	);

	/**
	 * Log a message (WARN) about this provider being deprecated
	 */
	@LogMessage(level = WARN)
	@Message(
			value = "The Ehcache second-level cache provider for Hibernate is deprecated.  " +
					"See https://hibernate.atlassian.net/browse/HHH-12441 for details.",
			id = 20100
	)
	void logDeprecation();

}
