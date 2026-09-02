/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation.jdbc;

import java.util.List;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;

/**
 * Descriptor for a table insert originating from a flush
 *
 * @author Steve Ebersole
 */
public final class JdbcInsertMutation extends AbstractJdbcMutation {
	public JdbcInsertMutation(
			TableMapping tableDetails,
			MutationTarget mutationTarget,
			String sql,
			boolean callable,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, callable, expectation, parameterBinders );
	}

	@Override
	public final MutationType getMutationType() {
		return MutationType.INSERT;
	}

	@Override
	public String toString() {
		return "JdbcInsertMutation(" + getTableDetails().getTableName() + ")";
	}
}
