/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqlAstQueryPartProcessingStateImpl
		extends SqlAstProcessingStateImpl
		implements SqlAstQueryPartProcessingState {

	private final QueryPart queryPart;
	private final boolean deduplicateSelectionItems;

	public SqlAstQueryPartProcessingStateImpl(
			QueryPart queryPart,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess,
			boolean deduplicateSelectionItems) {
		super( parent, creationState, currentClauseAccess );
		this.queryPart = queryPart;
		this.deduplicateSelectionItems = deduplicateSelectionItems;
	}

	public SqlAstQueryPartProcessingStateImpl(
			QueryPart queryPart,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Function<SqlExpressionResolver, SqlExpressionResolver> expressionResolverDecorator,
			Supplier<Clause> currentClauseAccess,
			boolean deduplicateSelectionItems) {
		super( parent, creationState, expressionResolverDecorator, currentClauseAccess );
		this.queryPart = queryPart;
		this.deduplicateSelectionItems = deduplicateSelectionItems;
	}

	@Override
	public QueryPart getInflightQueryPart() {
		return queryPart;
	}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	private Map<Expression, SqlSelection> sqlSelectionMap;

	@Override
	protected Map<Expression, SqlSelection> sqlSelectionMap() {
		return sqlSelectionMap;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			TypeConfiguration typeConfiguration) {
		final SqlSelection existing;
		if ( sqlSelectionMap == null ) {
			sqlSelectionMap = new HashMap<>();
			existing = null;
		}
		else {
			existing = sqlSelectionMap.get( expression );
		}

		if ( existing != null && deduplicateSelectionItems ) {
			return existing;
		}

		final SelectClause selectClause = ( (QuerySpec) queryPart ).getSelectClause();
		final int valuesArrayPosition = selectClause.getSqlSelections().size();
		final SqlSelection sqlSelection = expression.createSqlSelection(
				valuesArrayPosition + 1,
				valuesArrayPosition,
				javaType,
				typeConfiguration
		);

		sqlSelectionMap.put( expression, sqlSelection );

		selectClause.addSqlSelection( sqlSelection );

		return sqlSelection;
	}
}
