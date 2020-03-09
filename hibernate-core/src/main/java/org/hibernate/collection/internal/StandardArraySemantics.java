/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.collection.internal.ArrayInitializerProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * CollectionSemantics implementation for arrays
 *
 * @author Steve Ebersole
 */
public class StandardArraySemantics implements CollectionSemantics<Object[]> {
	/**
	 * Singleton access
	 */
	public static final StandardArraySemantics INSTANCE = new StandardArraySemantics();

	private StandardArraySemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ARRAY;
	}

	@Override
	public Class<Object[]> getCollectionJavaType() {
		return Object[].class;
	}

	@Override
	public Object[] instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
//		return (Object[]) Array.newInstance(
//				collectionDescriptor.getJavaTypeDescriptor().getJavaType().getComponentType(),
//				anticipatedSize
//		);
		throw new UnsupportedOperationException();
	}


	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentArrayHolder( session, collectionDescriptor );
	}

	@Override
	public PersistentCollection wrap(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentArrayHolder( session, rawCollection );
	}

	@Override
	public Iterator getElementIterator(Object[] rawCollection) {
		return Arrays.stream( rawCollection ).iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(Object[] array, Consumer action) {
		if ( array == null ) {
			return;
		}

		for ( Object element : array ) {
			action.accept( element );
		}
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState) {
		return new ArrayInitializerProducer(
				attributeMapping,
				attributeMapping.getIndexDescriptor().generateFetch(
						fetchParent,
						navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
						FetchTiming.IMMEDIATE,
						selected,
						lockMode,
						null,
						creationState
				),
				attributeMapping.getElementDescriptor().generateFetch(
						fetchParent,
						navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
						FetchTiming.IMMEDIATE,
						selected,
						lockMode,
						null,
						creationState
				)
		);
	}
}
