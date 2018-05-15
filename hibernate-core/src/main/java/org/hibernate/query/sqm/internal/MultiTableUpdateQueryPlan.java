/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.consume.multitable.spi.UpdateHandler;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class MultiTableUpdateQueryPlan implements NonSelectQueryPlan {
	private final UpdateHandler updateHandler;

	public MultiTableUpdateQueryPlan(UpdateHandler updateHandler) {
		this.updateHandler = updateHandler;
	}

	@Override
	public int executeUpdate(
			SharedSessionContractImplementor session,
			QueryOptions queryOptions,
			ParameterBindingContext parameterBindingContext) {
		return updateHandler.execute(
				new HandlerExecutionContext() {
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
				}
		);
	}
}
