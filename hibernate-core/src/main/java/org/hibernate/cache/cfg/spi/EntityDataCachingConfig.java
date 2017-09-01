/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

/**
 * Specialized DomainDataCachingConfig describing the requested
 * caching config for a particular entity hierarchy's state data
 *
 * @author Steve Ebersole
 */
public interface EntityDataCachingConfig extends DomainDataCachingConfig {
	/**
	 * The entity hierarchy whose data is to be cached as defined here.
	 */
	EntityHierarchy getEntityHierarchy();
}
