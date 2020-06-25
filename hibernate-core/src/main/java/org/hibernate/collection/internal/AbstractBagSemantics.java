/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.BagInitializerProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBagSemantics<B extends Collection<?>> implements CollectionSemantics<B> {
	@Override
	public Class<B> getCollectionJavaType() {
		//noinspection unchecked
		return (Class) Collection.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		if ( anticipatedSize < 1 ) {
			return (B) new ArrayList();
		}
		else {
			return (B) CollectionHelper.arrayList( anticipatedSize );
		}
	}

	@Override
	public Iterator getElementIterator(B rawCollection) {
		if ( rawCollection == null ) {
			return null;
		}
		return rawCollection.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(B rawCollection, Consumer action) {
		if ( rawCollection != null ) {
			rawCollection.forEach( action );
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
		return new BagInitializerProducer(
				attributeMapping,
				attributeMapping.getIdentifierDescriptor() == null ? null : attributeMapping.getIdentifierDescriptor().generateFetch(
						fetchParent,
						navigablePath.append( CollectionPart.Nature.ID.getName() ),
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

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState){
		if ( indexFetch == null ) {
			indexFetch = attributeMapping.getIdentifierDescriptor() == null ? null : attributeMapping.getIdentifierDescriptor().generateFetch(
					fetchParent,
					navigablePath.append( CollectionPart.Nature.ID.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					lockMode,
					null,
					creationState
			);
		}
		if ( elementFetch == null ) {
			elementFetch = attributeMapping.getElementDescriptor().generateFetch(
					fetchParent,
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					lockMode,
					null,
					creationState
			);
		}
		return new BagInitializerProducer(
				attributeMapping,
				indexFetch,
				elementFetch
		);
	}

}
