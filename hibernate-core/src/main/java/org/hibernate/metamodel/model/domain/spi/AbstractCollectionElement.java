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
	private final PersistentCollectionDescriptor persister;
	private final NavigableRole navigableRole;

	public AbstractCollectionElement(PersistentCollectionDescriptor persister) {
		this.persister = persister;
		this.navigableRole = persister.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public PersistentCollectionDescriptor getContainer() {
		return persister;
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
