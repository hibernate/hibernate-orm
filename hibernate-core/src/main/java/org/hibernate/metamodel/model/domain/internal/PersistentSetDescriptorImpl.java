/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.SetInitializerProducer;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * Hibernate's standard PersistentCollectionDescriptor implementor
 * for Lists
 *
 * @author Steve Ebersole
 */
public class PersistentSetDescriptorImpl<O,E> extends AbstractPersistentCollectionDescriptor<O,Set<E>,E> {
	public PersistentSetDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, context );
	}

	@Override
	protected CollectionInitializerProducer createInitializerProducer(
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		final NavigableReference navigableReference = creationState.getNavigableReferenceStack().getCurrent();
		return new SetInitializerProducer(
				this,
				selected,
				getElementDescriptor().createDomainResult(
						navigableReference,
						null,
						creationState, creationContext
				)
		);
	}

	@Override
	protected AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		return new SetAttributeImpl<>( this, pluralProperty, propertyAccess, creationContext );
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		return ( (Set) collection ).contains( childObject );
	}

	@Override
	public String toString() {
		return getNavigableRole().getFullPath();
	}
}
