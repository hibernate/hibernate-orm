/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

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

	enum RegionAccessType {
		ENTITY,
		NATURAL_ID,
		COLLECTION,
		QUERY_RESULTS,
		TIMESTAMPS
	}
}
