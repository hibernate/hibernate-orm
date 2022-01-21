/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Contract for things that can produce the {@link TableGroup} that is the root of a
 * from-clause
 *
 * @author Steve Ebersole
 */
public interface RootTableGroupProducer extends TableGroupProducer, ModelPartContainer {
	/**
	 * Create a root TableGroup as defined by this producer
	 */
	TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState,
			SqlAstCreationContext creationContext);

	TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver expressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext);
}
