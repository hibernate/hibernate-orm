/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.internal.PersistentListDescriptorImpl;
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
public class ListInitializerProducer implements CollectionInitializerProducer {
	private final PersistentListDescriptorImpl listDescriptor;
	private final boolean joined;
	private final DomainResult listIndexResult;
	private final DomainResult elementResult;

	public ListInitializerProducer(
			PersistentListDescriptorImpl listDescriptor,
			boolean joined,
			DomainResult listIndexResult,
			DomainResult elementResult) {
		this.listDescriptor = listDescriptor;
		this.joined = joined;
		this.listIndexResult = listIndexResult;
		this.elementResult = elementResult;
	}

	@Override
	public CollectionInitializer produceInitializer(
			FetchParentAccess parentAccess,
			NavigablePath navigablePath, LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState,
			AssemblerCreationContext creationContext) {

		return new ListInitializer(
				listDescriptor,
				parentAccess,
				navigablePath,
				joined,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				listIndexResult.createResultAssembler(
						initializerConsumer,
						creationState,
						creationContext
				),
				elementResult.createResultAssembler(
						initializerConsumer,
						creationState,
						creationContext
				)
		);
	}
}
