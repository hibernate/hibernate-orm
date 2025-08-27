/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import java.util.List;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;

/**
 * Describes the update of a single table
 *
 * @author Steve Ebersole
 */
public class JdbcUpdateMutation extends AbstractJdbcMutation {
	public JdbcUpdateMutation(
			TableMapping tableDetails,
			MutationTarget<?> mutationTarget,
			String sql,
			boolean callable,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, callable, expectation, parameterBinders );
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public String toString() {
		return "JdbcUpdateMutation(" + getTableDetails().getTableName() + ")";
	}
}
