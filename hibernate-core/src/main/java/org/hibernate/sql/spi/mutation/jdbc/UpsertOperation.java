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
 * {@link JdbcMutationOperation} implementation for UPSERT handling
 *
 * @author Steve Ebersole
 */
public final class UpsertOperation extends AbstractJdbcMutation {
	public UpsertOperation(
			TableMapping tableDetails,
			MutationTarget mutationTarget,
			String sql,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, false, expectation, parameterBinders );
	}

	@Override
	public final MutationType getMutationType() {
		return MutationType.UPDATE;
	}

}
