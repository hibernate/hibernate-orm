/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class DeleteQueryPlanImpl implements NonSelectQueryPlan {
	private final DeleteHandler deleteHandler;
	private final ExecutionContext executionContext;

	public DeleteQueryPlanImpl(
			SqmDeleteStatement sqmDeleteStatement,
			DeleteHandler deleteHandler,
			ExecutionContext executionContext) {
		this.deleteHandler = deleteHandler;
		this.executionContext = executionContext;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		return deleteHandler.execute( this.executionContext );
	}
}
