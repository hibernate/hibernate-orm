/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

/**
 * Producer for {@link Initializer} based on a {@link FetchParent}.
 *
 * @see AssemblerCreationState#resolveInitializer(FetchParent, InitializerParent, InitializerProducer)
 * @since 6.5
 */
public interface InitializerProducer<P extends FetchParent> {
	Initializer<?> createInitializer(
			P resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState);
}
