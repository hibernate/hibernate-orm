/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionIndex<J> implements CollectionIndex<J> {
	private final PersistentCollectionDescriptor descriptor;
	private final NavigableRole navigableRole;

	public AbstractCollectionIndex(PersistentCollectionDescriptor descriptor) {
		this.descriptor = descriptor;
		this.navigableRole = descriptor.getNavigableRole().append( NAVIGABLE_NAME );
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
	public String asLoggableText() {
		return "PluralAttributeIndex(" + descriptor.getNavigableRole() + " [" + getJavaType() + "])";
	}
}
