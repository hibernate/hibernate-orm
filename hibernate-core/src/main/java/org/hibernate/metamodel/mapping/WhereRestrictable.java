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
 * Things which can have {@link org.hibernate.annotations.SQLRestriction}
 * declarations - entities and collections
 *
 * @see FilterRestrictable
 */
public interface WhereRestrictable {

	/**
	 * Does this restrictable have a where restriction?
	 */
	boolean hasWhereRestrictions();

	/**
	 * Apply the {@link org.hibernate.annotations.SQLRestriction} restrictions
	 */
	void applyWhereRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState);
}
