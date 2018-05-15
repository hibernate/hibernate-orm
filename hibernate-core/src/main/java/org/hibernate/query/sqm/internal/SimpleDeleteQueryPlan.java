/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.sql.Connection;
import java.util.Collections;
import java.util.Set;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstDeleteDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmDeleteToSqlAstConverterSimple;
import org.hibernate.sql.ast.tree.spi.DeleteStatement;
import org.hibernate.sql.exec.internal.JdbcMutationExecutorImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan implements NonSelectQueryPlan {
	private final SqmDeleteStatement sqmStatement;

	public SimpleDeleteQueryPlan(SqmDeleteStatement sqmStatement) {
		this.sqmStatement = sqmStatement;

		// todo (6.0) : here is where we need to perform the conversion into SQL AST
	}

	@Override
	public int executeUpdate(
			SharedSessionContractImplementor session,
			QueryOptions queryOptions,
			ParameterBindingContext parameterBindingContext) {
		final DeleteStatement deleteStatement = SqmDeleteToSqlAstConverterSimple.interpret(
				sqmStatement,
				queryOptions,
				new SqlAstProducerContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return session.getFactory();
					}

					@Override
					public LoadQueryInfluencers getLoadQueryInfluencers() {
						return session.getLoadQueryInfluencers();
					}

					@Override
					public Callback getCallback() {
						return afterLoadAction -> {
						};
					}
				}
		);


		final JdbcMutation jdbcDelete = SqlDeleteToJdbcDeleteConverter.interpret(
				new SqlAstDeleteDescriptor() {
					@Override
					public DeleteStatement getSqlAstStatement() {
						return deleteStatement;
					}

					@Override
					public Set<String> getAffectedTableNames() {
						return Collections.singleton(
								deleteStatement.getTargetTable().getTable().getTableExpression()
						);
					}
				},
				session.getSessionFactory()
		);

		return JdbcMutationExecutorImpl.WITH_AFTER_STATEMENT_CALL.execute(
				jdbcDelete,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return queryOptions;
					}

					@Override
					public ParameterBindingContext getParameterBindingContext() {
						return parameterBindingContext;
					}

					@Override
					public Callback getCallback() {
						return afterLoadAction -> {};
					}
				},
				Connection::prepareStatement
		);
	}
}
