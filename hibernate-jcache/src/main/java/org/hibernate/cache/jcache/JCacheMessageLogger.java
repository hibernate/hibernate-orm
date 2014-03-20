/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import org.hibernate.internal.CoreMessageLogger;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Alex Snaps
 */
@MessageLogger(projectCode = "HHH")
public interface JCacheMessageLogger extends CoreMessageLogger {

	static final int NAMESPACE = 40000;

	@LogMessage(level = WARN)
	@Message(
			value = "Attempt to restart an already started JCacheRegionFactory.  Use sessionFactory.close() between " +
					"repeated calls to buildSessionFactory. Using previously created JCacheRegionFactory.",
			id = NAMESPACE + 1
	)
	void attemptToRestartAlreadyStartedJCacheProvider();

	@LogMessage(level = WARN)
	@Message(
			value = "Attempt to restop an already stopped JCacheRegionFactory.",
			id = NAMESPACE + 2
	)
	void attemptToRestopAlreadyStoppedJCacheProvider();
}
