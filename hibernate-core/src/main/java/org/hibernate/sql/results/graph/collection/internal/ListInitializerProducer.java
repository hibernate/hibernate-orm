/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;

/**
 * @author Steve Ebersole
 */
public class ListInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping attributeMapping;
	private final Fetch listIndexFetch;
	private final Fetch elementFetch;

	public ListInitializerProducer(
			PluralAttributeMapping attributeMapping,
			Fetch listIndexFetch,
			Fetch elementFetch) {
		this.attributeMapping = attributeMapping;
		this.listIndexFetch = listIndexFetch;
		this.elementFetch = elementFetch;
	}

	@Override
	public CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		return new ListInitializer(
				navigablePath,
				attributeMapping,
				parentAccess,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				listIndexFetch.createAssembler(
						parentAccess,
						initializerConsumer,
						creationState
				),
				elementFetch.createAssembler(
						parentAccess,
						initializerConsumer,
						creationState
				)
		);
	}
}
