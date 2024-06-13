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
import org.hibernate.sql.results.graph.InitializerParent;
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
	 * @param parent  may be null to indicate that the initializer is
	 *        for a {@link org.hibernate.sql.results.graph.DomainResult}
	 *        rather than a {@link org.hibernate.sql.results.graph.Fetch}
	 * @param collectionKeyResult allows creation of a
	 *        {@link org.hibernate.sql.results.graph.DomainResult} for
	 *        either side of the collection foreign key
	 * @param collectionValueKeyResult allows creation of a
	 *        {@link org.hibernate.sql.results.graph.DomainResult} for
	 *        either side of the collection foreign key
	 */
	CollectionInitializer<?> produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState);
}
