/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SimpleFromClauseAccessImpl;

/**
 * SqlAstCreationState and DomainResultCreationState implementation
 * for use while building various SQL AST trees related to collections
 *
 * @author Steve Ebersole
 */
public class SqlAstCreationStateImpl implements DomainResultCreationState, SqlAstCreationState {
	private final SessionFactoryImplementor sessionFactory;
	private final SqlAliasBaseGenerator sqlAliasBaseGenerator = new SqlAliasBaseManager();
	private final FromClauseAccess fromClauseAccess = new SimpleFromClauseAccessImpl();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	public SqlAstCreationStateImpl(SessionFactoryImplementor sessionFactory, QuerySpec querySpec) {
		this.sessionFactory = sessionFactory;
		processingStateStack.push(
				new SqlAstQuerySpecProcessingStateImpl(
						querySpec,
						null,
						this,
						() -> null,
						() -> expression -> {},
						() -> sqlSelection -> {}
				)
		);
	}

	@Override
	public SqlAstCreationContext getCreationContext() {
		return sessionFactory;
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return processingStateStack.getCurrent();
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseGenerator;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return getCurrentProcessingState().getSqlExpressionResolver();
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return Collections.emptyList();
	}

	@Override
	public boolean fetchAllAttributes() {
		return false;
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return LockMode.NONE;
	}
}
