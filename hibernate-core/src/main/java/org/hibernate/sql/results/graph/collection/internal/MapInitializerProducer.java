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
public class MapInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping mapDescriptor;
	private final boolean isJoined;
	private final Fetch mapKeyFetch;
	private final Fetch mapValueFetch;

	public MapInitializerProducer(
			PluralAttributeMapping mapDescriptor,
			boolean isJoined,
			Fetch mapKeyFetch,
			Fetch mapValueFetch) {
		this.mapDescriptor = mapDescriptor;
		this.isJoined = isJoined;
		this.mapKeyFetch = mapKeyFetch;
		this.mapValueFetch = mapValueFetch;
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
		final DomainResultAssembler mapKeyAssembler = mapKeyFetch.createAssembler(
				parentAccess,
				initializerConsumer,
				creationState
		);

		final DomainResultAssembler mapValueAssembler = mapValueFetch.createAssembler(
				parentAccess,
				initializerConsumer,
				creationState
		);

		return new MapInitializer(
				navigablePath,
				mapDescriptor,
				parentAccess,
				isJoined,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				mapKeyAssembler,
				mapValueAssembler
		);
	}
}
