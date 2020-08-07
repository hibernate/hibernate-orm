/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;

/**
 * SqlAstCreationState implementation for result-set mapping handling
 *
 * @author Steve Ebersole
 */
public class SqlAstCreationStateImpl implements SqlAstCreationState {
	private final SessionFactoryImplementor sessionFactory;
	private final FromClauseAccessImpl fromClauseAccess;
	private final SqlAstProcessingStateImpl processingState;

	public SqlAstCreationStateImpl(
			FromClauseAccessImpl fromClauseAccess,
			SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.fromClauseAccess = fromClauseAccess;
		this.processingState = new SqlAstProcessingStateImpl( this, fromClauseAccess );
	}

	@Override
	public SqlAstCreationContext getCreationContext() {
		return sessionFactory;
	}

	@Override
	public SqlAstProcessingStateImpl getCurrentProcessingState() {
		return processingState;
	}

	@Override
	public SqlAstProcessingStateImpl getSqlExpressionResolver() {
		return processingState;
	}

	@Override
	public FromClauseAccessImpl getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return stem -> {
			throw new UnsupportedOperationException();
		};
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return null;
	}
}
