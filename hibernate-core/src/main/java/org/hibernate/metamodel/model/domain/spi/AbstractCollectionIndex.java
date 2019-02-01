/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionIndex<J> implements CollectionIndex<J> {
	private final PersistentCollectionDescriptor descriptor;
	private final NavigableRole navigableRole;
	private final int baseIndex;
	private final boolean indexSettable;

	public AbstractCollectionIndex(PersistentCollectionDescriptor descriptor, IndexedCollection bootCollectionMapping) {
		this.descriptor = descriptor;
		this.navigableRole = descriptor.getNavigableRole().append( NAVIGABLE_NAME );

		if ( bootCollectionMapping.isList() ) {
			this.baseIndex = ( (org.hibernate.mapping.List) bootCollectionMapping ).getBaseIndex();
		}
		else {
			this.baseIndex = 0;
		}

		this.indexSettable = resolveIfIndexSettable( bootCollectionMapping );
	}

	@Override
	public PersistentCollectionDescriptor getContainer() {
		return descriptor;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public int getBaseIndex() {
		return baseIndex;
	}

	@Override
	public boolean isSettable() {
		return indexSettable;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeIndex(" + descriptor.getNavigableRole() + " [" + getJavaType() + "])";
	}

	private static boolean resolveIfIndexSettable(IndexedCollection bootCollectionMapping) {
		for ( int i = 0; i < bootCollectionMapping.getIndex().getColumnSpan(); ++i ) {
			if ( bootCollectionMapping.getIndex().getColumnInsertability()[i] ) {
				return true;
			}
			if ( bootCollectionMapping.getIndex().getColumnUpdateability()[i] ) {
				return true;
			}
		}
		return false;
	}
}
