/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
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
public class SetInitializerProducer implements CollectionInitializerProducer {
	private final PersistentCollectionDescriptor setDescriptor;
	private final boolean isSelected;
	private final DomainResult elementResult;

	public SetInitializerProducer(
			PersistentCollectionDescriptor setDescriptor,
			boolean isSelected,
			DomainResult elementResult) {
		this.setDescriptor = setDescriptor;
		this.elementResult = elementResult;
		this.isSelected = isSelected;
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
		final DomainResultAssembler elementAssembler = elementResult.createResultAssembler(
				initializerConsumer,
				creationState,
				creationContext
		);

		return new SetInitializer(
				setDescriptor,
				parentAccess,
				navigablePath,
				isSelected,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				elementAssembler
		);
	}
}
