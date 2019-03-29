/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class MultiTableUpdateQueryPlan implements NonSelectQueryPlan {
	private final UpdateHandler updateHandler;

	public MultiTableUpdateQueryPlan(UpdateHandler updateHandler) {
		this.updateHandler = updateHandler;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		return updateHandler.execute( executionContext );
	}
}
