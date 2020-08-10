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
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SqlAstCreationState implementation for result-set mapping handling
 *
 * @author Steve Ebersole
 */
@Internal
public class SqlAstCreationStateImpl implements SqlAstCreationState, SqlAstProcessingState, SqlExpressionResolver {

	private final FromClauseAccessImpl fromClauseAccess;
	private final SqlAliasBaseManager sqlAliasBaseManager;

	private final Consumer<SqlSelection> sqlSelectionConsumer;
	private final Map<String,SqlSelectionImpl> sqlSelectionMap = new HashMap<>();

	private final SessionFactoryImplementor sessionFactory;

	public SqlAstCreationStateImpl(
			FromClauseAccessImpl fromClauseAccess,
			SqlAliasBaseManager sqlAliasBaseManager,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		this.fromClauseAccess = fromClauseAccess;
		this.sqlAliasBaseManager = sqlAliasBaseManager;
		this.sqlSelectionConsumer = sqlSelectionConsumer;
		this.sessionFactory = sessionFactory;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstProcessingState

	@Override
	public SqlAstProcessingState getParentState() {
		return null;
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public SqlAstCreationContext getCreationContext() {
		return sessionFactory;
	}

	@Override
	public SqlAstCreationStateImpl getCurrentProcessingState() {
		return this;
	}

	@Override
	public SqlAstCreationStateImpl getSqlExpressionResolver() {
		return this;
	}

	@Override
	public FromClauseAccessImpl getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return LockMode.READ;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver


	@Override
	public Expression resolveSqlExpression(
			String key,
			Function<SqlAstProcessingState, Expression> creator) {
		return null;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return null;
	}
}
