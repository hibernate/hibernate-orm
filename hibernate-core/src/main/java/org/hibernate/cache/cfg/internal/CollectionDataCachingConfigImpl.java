/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import java.util.Comparator;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.VersionType;

/**
 * @author Steve Ebersole
 */
public class CollectionDataCachingConfigImpl
		extends AbstractDomainDataCachingConfig
		implements CollectionDataCachingConfig {
	private final Collection collectionDescriptor;
	private final NavigableRole navigableRole;

	public CollectionDataCachingConfigImpl(
			Collection collectionDescriptor,
			AccessType accessType) {
		super( accessType );
		this.collectionDescriptor = collectionDescriptor;
		this.navigableRole = new NavigableRole( collectionDescriptor.getRole() );
	}

	@Override
	public boolean isMutable() {
		return collectionDescriptor.isMutable();
	}

	@Override
	public boolean isVersioned() {
		return collectionDescriptor.getOwner().isVersioned();
	}

	@Override
	public Comparator getOwnerVersionComparator() {
		if ( !isVersioned() ) {
			return null;
		}
		return ( (VersionType) collectionDescriptor.getOwner().getVersion().getType() ).getComparator();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}
}
