/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
