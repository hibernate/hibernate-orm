/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;

/**
 * Simple implementation of ExecutionContext taking just a Session
 *
 * @author Steve Ebersole
 */
public class BasicExecutionContext implements ExecutionContext {
	private final SharedSessionContractImplementor session;
	private final ParameterBindingContext parameterBindingContext;
	private final JdbcParameterBindings jdbcParameterBindings;

	/**
	 * Full constructor
	 */
	public BasicExecutionContext(
			SharedSessionContractImplementor session,
			ParameterBindingContext parameterBindingContext,
			JdbcParameterBindings jdbcParameterBindings) {
		this.session = session;
		this.parameterBindingContext = parameterBindingContext;
		this.jdbcParameterBindings = jdbcParameterBindings;
	}

	public BasicExecutionContext(SharedSessionContractImplementor session) {
		this( session, JdbcParameterBindingsImpl.NO_BINDINGS );
	}

	public BasicExecutionContext(
			SharedSessionContractImplementor session,
			JdbcParameterBindings jdbcParameterBindings) {
		this(
				session,
				new ParameterBindingContext() {
					@Override
					public <T> List<T> getLoadIdentifiers() {
						return Collections.emptyList();
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return session.getSessionFactory();
					}
				},
				jdbcParameterBindings
		);
	}

	@Override
	public JdbcParameterBindings getJdbcParameterBindings() {
		return jdbcParameterBindings;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return session;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return QueryOptions.NONE;
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
