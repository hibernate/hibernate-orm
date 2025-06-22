/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Anything that has a discriminator associated with it.
 */
public interface Discriminable {
	DiscriminatorMapping getDiscriminatorMapping();

	/**
	 * Apply the discriminator as a predicate via the {@code predicateConsumer}
	 */
	void applyDiscriminator(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState creationState);
}
