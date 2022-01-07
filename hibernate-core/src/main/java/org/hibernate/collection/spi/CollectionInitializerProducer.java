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
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * Functional contract to create a CollectionInitializer
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
@FunctionalInterface
public interface CollectionInitializerProducer {
	/**
	 * Create an initializer for `attribute` relative to `navigablePath`.
	 *
	 * `parentAccess` may be null to indicate that the initializer is for
	 * a {@link org.hibernate.sql.results.graph.DomainResult} rather than
	 * a {@link org.hibernate.sql.results.graph.Fetch}
	 *
	 * `collectionKeyAssembler` and `collectionValueKeyAssembler` allow
	 * creating {@link org.hibernate.sql.results.graph.DomainResult} for
	 * either side of the collection foreign-key
	 */
	CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			AssemblerCreationState creationState);
}
