/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;
import java.util.Map;

/**
 * Second level cache statistics of a specific region
 *
 * @author Gavin King
 */
public interface SecondLevelCacheStatistics extends Serializable {
	
	long getHitCount();

	long getMissCount();

	long getPutCount();

	long getElementCountInMemory();

	long getElementCountOnDisk();

	long getSizeInMemory();

	Map getEntries();
}
