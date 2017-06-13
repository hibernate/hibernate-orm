/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal.sqm;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * @author Steve Ebersole
 */
public class MultiTableUpdateQueryPlan implements NonSelectQueryPlan {
	private final MultiTableBulkIdStrategy.UpdateHandler updateHandler;

	public MultiTableUpdateQueryPlan(MultiTableBulkIdStrategy.UpdateHandler updateHandler) {
		this.updateHandler = updateHandler;
	}

	@Override
	public int executeUpdate(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		return updateHandler.execute( inputParameterBindings, persistenceContext );
	}
}
