/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * Specialized DomainDataCachingConfig describing the requested
 * caching config for a particular entity hierarchy's state data
 *
 * @author Steve Ebersole
 */
public interface EntityDataCachingConfig extends DomainDataCachingConfig {
	/**
	 * Mainly here to allow optimization of not having to know the
	 * actual comparator instance to use here yet.  If this method
	 * returns {@code true}, then users can safely assume that
	 * accessing {@link #getVersionComparatorAccess()} will
	 * not produce a null Comparator later
	 *
	 */
	boolean isVersioned();

	/**
	 * Access to the comparator to be used with the entity's
	 * version.  If the entity is not versioned, then this method
	 * returns {@code null}.
	 */
	Supplier<Comparator> getVersionComparatorAccess();

	/**
	 * The list of specific subclasses of the root that are actually
	 * written to cache.
	 */
	Set<NavigableRole> getCachedTypes();

	// todo (5.3) : what else is needed here?
}
