/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class NativeNonSelectQueryPlanImpl implements NonSelectQueryPlan {
	private final NativeQueryImplementor nativeQuery;

	public NativeNonSelectQueryPlanImpl(NativeQueryImplementor nativeQuery) {
		this.nativeQuery = nativeQuery;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception();
	}
}
