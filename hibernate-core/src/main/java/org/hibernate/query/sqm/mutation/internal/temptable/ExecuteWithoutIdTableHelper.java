/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
public final class ExecuteWithoutIdTableHelper {
	private ExecuteWithoutIdTableHelper() {
	}

	public static QuerySpec createIdMatchingSubQuerySpec(
			NavigablePath navigablePath,
			TableReference rootTableReference,
			Predicate predicate,
			EntityPersister rootEntityPersister,
			SqlExpressionResolver sqlExpressionResolver,
			SessionFactoryImplementor sessionFactory) {
		/*
		 * `select root_id from root_table where {predicate}
		 */
		final QuerySpec matchingIdSelect = new QuerySpec( false, 1 );

		final StandardTableGroup matchingIdSelectTableGroup = new StandardTableGroup(
				true,
				navigablePath,
				rootEntityPersister,
				rootTableReference.getIdentificationVariable(),
				rootTableReference,
				null,
				sessionFactory
		);

		matchingIdSelect.getFromClause().addRoot( matchingIdSelectTableGroup );

		rootEntityPersister.getIdentifierMapping().forEachSelectable(
				(columnIndex, selection) -> {
					final ColumnReference columnReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
							rootTableReference,
							selection
					);
					final SqlSelection sqlSelection = new SqlSelectionImpl( columnReference );
					matchingIdSelect.getSelectClause().addSqlSelection( sqlSelection );
				}
		);

		matchingIdSelect.applyPredicate( predicate );

		return matchingIdSelect;
	}
}
