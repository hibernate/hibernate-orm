/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Describes the semantics of a persistent collection such that Hibernate
 * understands how to use it - create one, handle elements, etc.
 *
 * @apiNote The described collection need not be part of the "Java Collection Framework"
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
public interface CollectionSemantics<C> {
	/**
	 * Get the classification of collections described by this semantic
	 */
	CollectionClassification getCollectionClassification();

	Class<C> getCollectionJavaType();

	C instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor);

	PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session);

	PersistentCollection wrap(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session);

	Iterator getElementIterator(C rawCollection);

	void visitElements(C rawCollection, Consumer action);

	/**
	 * todo (6.0) : clean this contract up!
	 */
	CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState);

	CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState);
}
