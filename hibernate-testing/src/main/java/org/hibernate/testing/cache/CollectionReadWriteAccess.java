/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Chris Cranford
 */
public class CollectionReadWriteAccess extends BaseCollectionDataAccess {
	public CollectionReadWriteAccess(
			DomainDataRegionImpl region,
			PersistentCollectionDescriptor collectionDescriptor) {
		super( region, collectionDescriptor );
	}
}
