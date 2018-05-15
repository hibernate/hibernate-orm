/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.internal.PersistentMapDescriptorImpl;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class MapInitializerProducer implements CollectionInitializerProducer {
	private final PersistentMapDescriptorImpl mapDescriptor;
	private final boolean isJoined;
	private final DomainResult mapKeyResult;
	private final DomainResult mapValueResult;

	public MapInitializerProducer(
			PersistentMapDescriptorImpl mapDescriptor,
			boolean isJoined,
			DomainResult mapKeyResult,
			DomainResult mapValueResult) {
		this.mapDescriptor = mapDescriptor;
		this.isJoined = isJoined;
		this.mapKeyResult = mapKeyResult;
		this.mapValueResult = mapValueResult;
	}

	@Override
	public CollectionInitializer produceInitializer(
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState,
			AssemblerCreationContext creationContext) {
		final DomainResultAssembler mapKeyAssembler = mapKeyResult.createResultAssembler(
				initializerConsumer,
				creationState,
				creationContext
		);

		final DomainResultAssembler mapValueAssembler = mapValueResult.createResultAssembler(
				initializerConsumer,
				creationState,
				creationContext
		);

		return new MapInitializer(
				mapDescriptor,
				parentAccess,
				navigablePath,
				isJoined,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				mapKeyAssembler,
				mapValueAssembler
		);
	}
}
