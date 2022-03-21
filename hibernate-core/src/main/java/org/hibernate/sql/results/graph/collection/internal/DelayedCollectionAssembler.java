/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionAssembler extends AbstractCollectionAssembler {
	public DelayedCollectionAssembler(
			NavigablePath fetchPath,
			PluralAttributeMapping fetchedMapping,
			FetchParentAccess parentAccess,
			DomainResult<?> collectionKeyResult,
			AssemblerCreationState creationState) {
		super(
				fetchedMapping,
				() -> (CollectionInitializer) creationState.resolveInitializer(
						fetchPath,
						fetchedMapping,
						() -> {
							final DomainResultAssembler<?> collectionKeyAssembler = collectionKeyResult.createResultAssembler(
									parentAccess,
									creationState
							);
							return new DelayedCollectionInitializer(
									fetchPath,
									fetchedMapping,
									parentAccess,
									collectionKeyAssembler
							);
						}
				)

		);
	}
}
