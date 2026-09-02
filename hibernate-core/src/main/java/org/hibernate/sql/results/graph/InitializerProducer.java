/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

/**
 * Producer for {@link Initializer} based on a {@link FetchParent}.
 *
 * @see AssemblerCreationState#resolveInitializer(FetchParent, InitializerParent, InitializerProducer)
 * @since 6.5
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface InitializerProducer<P extends FetchParent> {
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	Initializer<?> createInitializer(
			P resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState);
}
