/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.sql;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.support.domains.gambit.EntityOfBasics;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.results.spi.ResultSetMapping;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
public class SqlAstGenSmokeTest extends BaseSqmSqlTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityOfBasics.class );
	}

	@Test
	public void testSqlAstGeneration() {
		final SharedSessionContractImplementor session = (SharedSessionContractImplementor) getSessionFactory().openSession();

		try {
			final JdbcSelect jdbcSelect = buildJdbcSelect(
					"select a.theString from EntityOfBasics a",
					new ExecutionContext() {
						private final QueryOptions queryOptions = new QueryOptionsImpl();

						private final QueryParameterBindings parameterBindings = new QueryParameterBindings() {
							@Override
							public boolean isBound(QueryParameterImplementor parameter) {
								return false;
							}

							@Override
							public QueryParameterBinding<?> getBinding(QueryParameterImplementor parameter) {
								return null;
							}

							@Override
							public QueryParameterBinding<?> getBinding(String name) {
								return null;
							}

							@Override
							public QueryParameterBinding<?> getBinding(int position) {
								return null;
							}

							@Override
							public void validate() {
							}
						};

						private final ParameterBindingContext parameterBindingContext = new ParameterBindingContext() {
							@Override
							public SessionFactoryImplementor getSessionFactory() {
								return sessionFactory();
							}

							@Override
							public <T> List<T> getLoadIdentifiers() {
								return Collections.emptyList();
							}

							@Override
							public QueryParameterBindings getQueryParameterBindings() {
								return parameterBindings;
							}
						};

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
							return afterLoadAction -> {

							};
						}
					}
			);

			assertThat( jdbcSelect.getAffectedTableNames(), hasSize(1) );

			final ResultSetMapping resultSetMapping = jdbcSelect.getResultSetMapping().resolve( null, null );
			assertThat( resultSetMapping.getDomainResults(), hasSize(1) );
		}
		finally {
			session.close();
		}
	}
}
