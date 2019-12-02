/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQuerySpecProcessingState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqlAstQuerySpecProcessingStateImpl
		extends SqlAstProcessingStateImpl
		implements SqlAstQuerySpecProcessingState {

	private final QuerySpec querySpec;

	public SqlAstQuerySpecProcessingStateImpl(
			QuerySpec querySpec,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, currentClauseAccess );
		this.querySpec = querySpec;
	}

	@Override
	public QuerySpec getInflightQuerySpec() {
		return querySpec;
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
			JavaTypeDescriptor javaTypeDescriptor,
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

		querySpec.getSelectClause().addSqlSelection( sqlSelection );

		return sqlSelection;
	}
}
