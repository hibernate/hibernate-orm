/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryOptions;

/**
 * @author Steve Ebersole
 */
public class NonSelectQueryPlanImpl implements NonSelectQueryPlan {
	public NonSelectQueryPlanImpl(NativeQueryImpl nativeQuery) {
	}

	@Override
	public int executeUpdate(
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		throw new NotYetImplementedException();
	}
}
