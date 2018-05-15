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
public abstract class AbstractCollectionElement<J> implements CollectionElement<J> {
	private final PersistentCollectionDescriptor descriptor;
	private final NavigableRole navigableRole;

	public AbstractCollectionElement(PersistentCollectionDescriptor descriptor) {
		this.descriptor = descriptor;
		this.navigableRole = descriptor.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public PersistentCollectionDescriptor getContainer() {
		return descriptor;
	}

	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return descriptor;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String asLoggableText() {
		return NAVIGABLE_NAME;
	}

}
