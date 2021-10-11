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
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqlAstQueryPartProcessingStateImpl
		extends SqlAstProcessingStateImpl
		implements SqlAstQueryPartProcessingState {

	private final QueryPart queryPart;

	public SqlAstQueryPartProcessingStateImpl(
			QueryPart queryPart,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, currentClauseAccess );
		this.queryPart = queryPart;
	}

	public SqlAstQueryPartProcessingStateImpl(
			QueryPart queryPart,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Function<SqlExpressionResolver, SqlExpressionResolver> expressionResolverDecorator,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, expressionResolverDecorator, currentClauseAccess );
		this.queryPart = queryPart;
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
			JavaType javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		final SqlSelection existing;
		if ( sqlSelectionMap == null ) {
			sqlSelectionMap = new HashMap<>();
			existing = null;
		}
		else {
			existing = sqlSelectionMap.get( expression );
		}

		if ( existing != null ) {
			return existing;
		}

		final int valuesArrayPosition = sqlSelectionMap.size();
		final SqlSelection sqlSelection = expression.createSqlSelection(
				valuesArrayPosition + 1,
				valuesArrayPosition,
				javaTypeDescriptor,
				typeConfiguration
		);

		sqlSelectionMap.put( expression, sqlSelection );

		( (QuerySpec) queryPart ).getSelectClause().addSqlSelection( sqlSelection );

		return sqlSelection;
	}
}
