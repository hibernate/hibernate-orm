/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class UpdateQueryPlanImpl implements NonSelectQueryPlan {
	private final UpdateHandler updateHandler;
	private final ExecutionContext executionContext;

	public <R> UpdateQueryPlanImpl(
			SqmUpdateStatement sqmStatement,
			UpdateHandler updateHandler,
			ExecutionContext executionContext) {
		this.updateHandler = updateHandler;
		this.executionContext = executionContext;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		return updateHandler.execute( this.executionContext );
	}
}
