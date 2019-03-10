/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.collection.BagInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class PersistentBagDescriptorImpl<O,E> extends AbstractPersistentCollectionDescriptor<O,Collection<E>,E> {
	public PersistentBagDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, context );
	}

	@Override
	protected CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState) {
		final DomainResult collectionIdResult;
		if ( getIdDescriptor() != null ) {
			collectionIdResult = getIdDescriptor().createDomainResult(
					null,
					creationState
			);
		}
		else {
			collectionIdResult = null;
		}

		final DomainResult elementResult = getElementDescriptor().createDomainResult(
				navigablePath,
				resultVariable,
				creationState
		);

		return new BagInitializerProducer(
				this,
				selected,
				collectionIdResult,
				elementResult
		);
	}

	@Override
	protected AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		return new BagAttributeImpl<>( this, pluralProperty, propertyAccess, creationContext );
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		return ( (Collection ) collection ).contains( childObject );
	}

	@Override
	protected void doProcessQueuedOps(
			PersistentCollection collection, Object id, SharedSessionContractImplementor session) {
		// do nothing
	}
}
