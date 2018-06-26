/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;


import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-jcache module.  It reserves message ids ranging from
 * 25099 to 25099 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 25001, max = 25099)
public interface JCacheMessageLogger extends CoreMessageLogger {
	JCacheMessageLogger INSTANCE = Logger.getMessageLogger(
			JCacheMessageLogger.class,
			"org.hibernate.orm.javax.cache"
	);

	@LogMessage(level = WARN)
	@Message(
			value = "Missing cache[%s] was created on-the-fly." +
					" The created cache will use a provider-specific default configuration:" +
					" make sure you defined one." +
					" You can disable this warning by setting '" + ConfigSettings.MISSING_CACHE_STRATEGY +
					"' to 'create'.",
			id = 25001
	)
	void missingCacheCreated(String regionName);

}
