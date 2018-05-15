/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class CollectionReadOnlyAccess extends BaseCollectionDataAccess {
	public CollectionReadOnlyAccess(
			DomainDataRegionImpl region,
			PersistentCollectionDescriptor collectionDescriptor) {
		super( region, collectionDescriptor );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}
}
