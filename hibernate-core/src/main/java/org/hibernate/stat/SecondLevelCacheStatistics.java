/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.stat;

import java.util.Collections;
import java.util.Map;

/**
 * Cache statistics pertaining to a specific data region
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated Use {@link CacheRegionStatistics} instead
 */
@Deprecated
public interface SecondLevelCacheStatistics extends CacheRegionStatistics {
	default Map getEntries() {
		return Collections.emptyMap();
	}
}
