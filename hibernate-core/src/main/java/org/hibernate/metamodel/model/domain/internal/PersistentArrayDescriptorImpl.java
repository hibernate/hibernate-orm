/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.domain.collection.ArrayInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class PersistentArrayDescriptorImpl<O,E> extends AbstractPersistentCollectionDescriptor<O,E[], E> {
	private final boolean hasFormula;

	public PersistentArrayDescriptorImpl(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext) {
		super( pluralProperty, runtimeContainer, creationContext );
		IndexedCollection collection = (IndexedCollection) pluralProperty.getValue();
		hasFormula = collection.getIndex().hasFormula();
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	protected CollectionJavaDescriptor resolveCollectionJtd(
//			Collection collectionBinding,
//			RuntimeModelCreationContext creationContext) {
//		Class componentType = getElementDescriptor().getJavaTypeDescriptor().getJavaType();
//		if ( componentType == null ) {
//			// MAP entity mode?
//			// todo (6.0) : verify this
//			componentType = Map.class;
//		}
//
//		// The only way I know to handle this is by instantiating an array...
//		// todo (6.0) : better way?
//		final Class arrayType = Array.newInstance( componentType, 0 ).getClass();
//		assert arrayType.isArray();
//
//		final CollectionJavaDescriptor javaDescriptor = new CollectionJavaDescriptor(
//				arrayType,
//				StandardArraySemantics.INSTANCE
//		);
//		creationContext.getTypeConfiguration().getJavaTypeDescriptorRegistry().addDescriptor( javaDescriptor );
//
//		return javaDescriptor;
//	}

	@Override
	protected CollectionInitializerProducer createInitializerProducer(
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		final NavigableReference navigableReference = creationState.getNavigableReferenceStack().getCurrent();

		return new ArrayInitializerProducer(
				this,
				selected,
				getIndexDescriptor().createDomainResult(
						navigableReference,
						null,
						creationState, creationContext
				),
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
		return new ListAttributeImpl<>( this, pluralProperty, propertyAccess, creationContext );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(Object collection, Object childObject) {
		assert collection.getClass().isArray();

		final int length = Array.getLength( collection );
		for ( int i = 0; i < length; i++ ) {
			if ( getElementDescriptor().getJavaTypeDescriptor().areEqual( Array.get( collection, i ), childObject ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected void doProcessQueuedOps(
			PersistentCollection collection, Object id, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	protected boolean hasIndex() {
		return true;
	}

	protected boolean indexContainsFormula(){
		return hasFormula;
	}

	@Override
	public Iterator getElementsIterator(Object collection, SharedSessionContractImplementor session) {
		return Arrays.asList( (Object[]) collection ).iterator();
	}

	@Override
	public Object indexOf(Object array, Object element) {
		int length = Array.getLength( array );
		for ( int i = 0; i < length; i++ ) {
			//TODO: proxies!
			if ( Array.get( array, i ) == element ) {
				return i;
			}
		}
		return null;
	}
}
