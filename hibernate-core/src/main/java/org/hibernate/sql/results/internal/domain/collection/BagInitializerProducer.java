/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.LockMode;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class BagInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping bagDescriptor;
	private final boolean selected;
	private final Fetch collectionIdFetch;
	private final Fetch elementFetch;

	public BagInitializerProducer(
			PluralAttributeMapping bagDescriptor,
			boolean selected,
			Fetch collectionIdFetch,
			Fetch elementFetch) {
		this.bagDescriptor = bagDescriptor;
		this.selected = selected;

		if ( bagDescriptor.getIdentifierDescriptor() != null ) {
			assert collectionIdFetch != null;
			this.collectionIdFetch = collectionIdFetch;
		}
		else {
			assert collectionIdFetch == null;
			this.collectionIdFetch = null;
		}

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
		final DomainResultAssembler elementAssembler = elementFetch.createAssembler(
				parentAccess,
				initializerConsumer,
				creationState
		);

		final DomainResultAssembler collectionIdAssembler;
		if ( bagDescriptor.getIdentifierDescriptor() == null ) {
			collectionIdAssembler = null;
		}
		else {
			collectionIdAssembler = collectionIdFetch.createAssembler(
					parentAccess,
					initializerConsumer,
					creationState
			);
		}

		return new BagInitializer(
				bagDescriptor,
				parentAccess,
				navigablePath,
				selected,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				elementAssembler,
				collectionIdAssembler
		);
	}
}
