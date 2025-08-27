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
 * @author Steve Ebersole
 */
public class JdbcDeleteMutation extends AbstractJdbcMutation {
	private final MutationType mutationType;

	public JdbcDeleteMutation(
			TableMapping tableDetails,
			MutationTarget<?> mutationTarget,
			String sql,
			boolean callable,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		this( tableDetails, MutationType.DELETE, mutationTarget, sql, callable, expectation, parameterBinders );
	}

	public JdbcDeleteMutation(
			TableMapping tableDetails,
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			String sql,
			boolean callable,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, callable, expectation, parameterBinders );
		this.mutationType = mutationType;
	}

	@Override
	public MutationType getMutationType() {
		return mutationType;
	}

	@Override
	public String toString() {
		return "JdbcDeleteMutation(" + getTableDetails().getTableName() + ")";
	}
}
