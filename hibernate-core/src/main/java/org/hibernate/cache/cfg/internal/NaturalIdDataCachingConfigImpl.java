/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import java.util.Iterator;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public class NaturalIdDataCachingConfigImpl
		extends AbstractDomainDataCachingConfig
		implements NaturalIdDataCachingConfig {
	private final RootClass rootEntityDescriptor;
	private final NavigableRole navigableRole;
	private final boolean mutable;

	public NaturalIdDataCachingConfigImpl(
			RootClass rootEntityDescriptor,
			AccessType accessType) {
		super( accessType );
		this.rootEntityDescriptor = rootEntityDescriptor;
		this.navigableRole = new NavigableRole( rootEntityDescriptor.getEntityName() );

		// sucks that we need to do this here.  persister does the same "calculation"
		this.mutable = hasAnyMutableNaturalIdProps();
	}

	private boolean hasAnyMutableNaturalIdProps() {
		final Iterator itr = rootEntityDescriptor.getDeclaredPropertyIterator();
		while ( itr.hasNext() ) {
			final Property prop = (Property) itr.next();
			if ( prop.isNaturalIdentifier() && prop.isUpdateable() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isVersioned() {
		return false;
	}
}
