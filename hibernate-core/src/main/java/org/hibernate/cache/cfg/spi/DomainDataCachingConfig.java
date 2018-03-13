/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * Configuration for a specific type of data to be stored in the
 * region
 *
 * @author Steve Ebersole
 */
public interface DomainDataCachingConfig {
	/**
	 * The requested AccessType
	 */
	AccessType getAccessType();

	/**
	 * Is the data marked as being mutable?
	 */
	boolean isMutable();

	/**
	 * Is the data to be cached considered versioned?
	 */
	boolean isVersioned();

	/**
	 * The {@link NavigableRole} of the thing to be cached
	 */
	NavigableRole getNavigableRole();
}
