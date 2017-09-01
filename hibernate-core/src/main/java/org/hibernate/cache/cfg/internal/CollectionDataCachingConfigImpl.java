/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionDataCachingConfigImpl
		extends AbstractDomainDataCachingConfig
		implements CollectionDataCachingConfig {
	private final PersistentCollectionDescriptor collectionDescriptor;

	public CollectionDataCachingConfigImpl(
			PersistentCollectionDescriptor collectionDescriptor,
			AccessType accessType) {
		super( accessType );
		this.collectionDescriptor = collectionDescriptor;
	}

	@Override
	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}
}
