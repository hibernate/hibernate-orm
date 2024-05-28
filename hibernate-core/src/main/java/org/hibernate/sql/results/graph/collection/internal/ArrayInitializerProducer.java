/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Chris Cranford
 */
public class ArrayInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping arrayDescriptor;
	private final Fetch listIndexFetch;
	private final Fetch elementFetch;

	public ArrayInitializerProducer(
			PluralAttributeMapping arrayDescriptor,
			Fetch listIndexFetch,
			Fetch elementFetch) {
		this.arrayDescriptor = arrayDescriptor;
		this.listIndexFetch = listIndexFetch;
		this.elementFetch = elementFetch;
	}

	@Override
	public CollectionInitializer<?> produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		return new ArrayInitializer(
				navigablePath,
				arrayDescriptor,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState,
				listIndexFetch,
				elementFetch
		);
	}
}
