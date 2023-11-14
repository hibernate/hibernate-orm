/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * Functional contract to create a {@link CollectionInitializer}.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
@FunctionalInterface
public interface CollectionInitializerProducer {
	/**
	 * Create an initializer for the given attribute relative to the given
	 * navigable path.
	 *
	 * @param navigablePath the navigable path
	 * @param attribute the attribute
	 * @param parentAccess  may be null to indicate that the initializer is
	 *        for a {@link org.hibernate.sql.results.graph.DomainResult}
	 *        rather than a {@link org.hibernate.sql.results.graph.Fetch}
	 * @param collectionKeyAssembler allows creation of a
	 *        {@link org.hibernate.sql.results.graph.DomainResult} for
	 *        either side of the collection foreign key
	 * @param collectionValueKeyAssembler allows creation of a
	 *        {@link org.hibernate.sql.results.graph.DomainResult} for
	 *        either side of the collection foreign key
	 * @deprecated Use {@link #produceInitializer(NavigablePath, PluralAttributeMapping, FetchParentAccess, LockMode, DomainResult, DomainResult, AssemblerCreationState)} instead
	 */
	@Deprecated(forRemoval = true)
	CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			AssemblerCreationState creationState);

	default CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		final DomainResultAssembler<?> collectionKeyAssembler;
		final DomainResultAssembler<?> collectionValueKeyAssembler;
		if ( collectionKeyResult == null ) {
			collectionKeyAssembler = null;
			collectionValueKeyAssembler = collectionValueKeyResult.createResultAssembler(
					null,
					creationState
			);
		}
		else if ( collectionKeyResult == collectionValueKeyResult ) {
			collectionKeyAssembler = collectionValueKeyAssembler = collectionKeyResult.createResultAssembler(
					null,
					creationState
			);
		}
		else {
			collectionKeyAssembler = collectionKeyResult.createResultAssembler(
					null,
					creationState
			);
			collectionValueKeyAssembler = collectionValueKeyResult.createResultAssembler(
					null,
					creationState
			);
		}
		return produceInitializer(
				navigablePath,
				attribute,
				parentAccess,
				lockMode,
				collectionKeyAssembler,
				collectionValueKeyAssembler,
				creationState
		);
	}
}
