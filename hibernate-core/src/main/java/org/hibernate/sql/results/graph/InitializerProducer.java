/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
