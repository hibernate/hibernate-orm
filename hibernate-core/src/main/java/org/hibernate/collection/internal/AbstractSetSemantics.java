/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.SetInitializerProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSetSemantics<SE extends Set<E>,E> implements CollectionSemantics<SE,E> {
	@Override
	public Class<? extends Set> getCollectionJavaType() {
		return Set.class;
	}

	@Override
	public Iterator<E> getElementIterator(SE rawCollection) {
		if ( rawCollection == null ) {
			return null;
		}
		return rawCollection.iterator();
	}

	@Override
	public void visitElements(SE rawCollection, Consumer<? super E> action) {
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
		return new SetInitializerProducer(
				attributeMapping,
				fetchParent.generateFetchableFetch(
						attributeMapping.getElementDescriptor(),
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
	public  CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState){
		if ( elementFetch == null ) {
			return createInitializerProducer(
					navigablePath,
					attributeMapping,
					fetchParent,
					selected,
					resultVariable,
					lockMode,
					creationState
			);
		}
		return new SetInitializerProducer( attributeMapping, elementFetch );
	}
}
