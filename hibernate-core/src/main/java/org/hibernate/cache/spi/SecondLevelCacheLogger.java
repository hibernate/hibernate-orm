/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.metamodel.model.domain.NavigableRole;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90001001, max = 90002000 )
public interface SecondLevelCacheLogger extends BasicLogger {
	SecondLevelCacheLogger INSTANCE = Logger.getMessageLogger(
			SecondLevelCacheLogger.class,
			"org.hibernate.orm.cache"
	);

	@LogMessage( level = WARN )
	@Message(
			value = "Read-only caching was requested for mutable entity [%s]",
			id = 90001001
	)
	void readOnlyCachingMutableEntity(NavigableRole navigableRole);

	@LogMessage( level = WARN )
	@Message(
			value = "Read-only caching was requested for mutable natural-id for entity [%s]",
			id = 90001002
	)
	void readOnlyCachingMutableNaturalId(NavigableRole navigableRole);

}
