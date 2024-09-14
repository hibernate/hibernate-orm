/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Contract for things that can produce the {@link TableGroup} that is a root of a
 * {@link FromClause#getRoots() from-clause}
 *
 * @author Steve Ebersole
 */
public interface RootTableGroupProducer extends TableGroupProducer {
	/**
	 * Create a root TableGroup as defined by this producer
	 *
	 * @param canUseInnerJoins Whether inner joins can be used when creating {@linkplain TableReference table-references} within this group
	 * @param navigablePath The overall NavigablePath for the root
	 * @param explicitSourceAlias The alias, if one, explicitly provided by the application for this root
	 * @param explicitSqlAliasBase A specific SqlAliasBase to use.  May be {@code null} indicating one should be created using the {@linkplain SqlAliasBaseGenerator} from {@code creationState}
	 * @param additionalPredicateCollectorAccess Collector for additional predicates associated with this producer
	 * @param creationState The creation state
	 */
	TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState);
}
