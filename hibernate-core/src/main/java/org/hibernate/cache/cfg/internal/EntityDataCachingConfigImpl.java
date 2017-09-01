/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

/**
 * @author Steve Ebersole
 */
public class EntityDataCachingConfigImpl
		extends AbstractDomainDataCachingConfig
		implements EntityDataCachingConfig {
	private final EntityHierarchy entityHierarchy;

	public EntityDataCachingConfigImpl(
			EntityHierarchy entityHierarchy,
			AccessType accessType) {
		super( accessType );
		this.entityHierarchy = entityHierarchy;
	}

	@Override
	public EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}
}
