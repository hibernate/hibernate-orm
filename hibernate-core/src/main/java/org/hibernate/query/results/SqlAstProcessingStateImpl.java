/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Internal;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
@Internal
public class SqlAstProcessingStateImpl implements SqlAstProcessingState, SqlExpressionResolver {
	private final FromClauseAccessImpl fromClauseAccess;

	private final Map<String,SqlSelectionImpl> sqlSelectionMap = new HashMap<>();
	private final Consumer<SqlSelection> sqlSelectionConsumer;

	private final SqlAstCreationState sqlAstCreationState;

	public SqlAstProcessingStateImpl(
			FromClauseAccessImpl fromClauseAccess,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SqlAstCreationState sqlAstCreationState) {
		this.fromClauseAccess = fromClauseAccess;
		this.sqlSelectionConsumer = sqlSelectionConsumer;
		this.sqlAstCreationState = sqlAstCreationState;
	}

	@Override
	public SqlAstProcessingState getParentState() {
		// none
		return null;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return this;
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return sqlAstCreationState;
	}

	public int getNumberOfProcessedSelections() {
		return sqlSelectionMap.size();
	}

	@Override
	public Expression resolveSqlExpression(
			String key,
			Function<SqlAstProcessingState, Expression> creator) {
		final SqlSelectionImpl existing = sqlSelectionMap.get( key );
		if ( existing != null ) {
			return existing;
		}

		final Expression created = creator.apply( this );

		if ( created instanceof SqlSelectionImpl ) {
			sqlSelectionMap.put( key, (SqlSelectionImpl) created );
			sqlSelectionConsumer.accept( (SqlSelectionImpl) created );
		}
		else if ( created instanceof ColumnReference ) {
			final ColumnReference columnReference = (ColumnReference) created;

			final SqlSelectionImpl sqlSelection = new SqlSelectionImpl(
					sqlSelectionMap.size() + 1,
					columnReference.getJdbcMapping()
			);

			sqlSelectionMap.put( key, sqlSelection );
			sqlSelectionConsumer.accept( sqlSelection );
		}

		return created;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		assert expression instanceof SqlSelectionImpl;
		return (SqlSelection) expression;
	}

	public FromClauseAccess getFromClauseAccess() {
		return fromClauseAccess;
	}
}
