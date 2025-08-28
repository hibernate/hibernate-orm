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
 * {@link JdbcMutationOperation} implementation for UPSERT handling
 *
 * @author Steve Ebersole
 */
public class UpsertOperation extends AbstractJdbcMutation {
	public UpsertOperation(
			TableMapping tableDetails,
			MutationTarget<?> mutationTarget,
			String sql,
			List<? extends JdbcParameterBinder> parameterBinders) {
		this( tableDetails, mutationTarget, sql, new Expectation.RowCount(), parameterBinders );
	}

	public UpsertOperation(
			TableMapping tableDetails,
			MutationTarget<?> mutationTarget,
			String sql,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, false, expectation, parameterBinders );
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}

}
