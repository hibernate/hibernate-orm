/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

import java.util.Comparator;

/**
 * Specialized DomainDataCachingConfig describing the requested
 * caching config for a particular persistent collection's data
 *
 * @author Steve Ebersole
 */
public interface CollectionDataCachingConfig extends DomainDataCachingConfig {
	/**
	 * The comparator to be used with the owning entity's version (if it has one).
	 */
	Comparator getOwnerVersionComparator();
}
