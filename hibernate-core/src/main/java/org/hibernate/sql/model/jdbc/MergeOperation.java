/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import java.util.List;

import org.hibernate.jdbc.Expectations;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;

/**
 * JdbcMutation implementation for MERGE handling
 *
 * @author Steve Ebersole
 */
public class MergeOperation extends AbstractJdbcMutation {
	public MergeOperation(
			TableMapping tableDetails,
			MutationTarget<?> mutationTarget,
			String sql,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, false, Expectations.NONE, parameterBinders );
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}


}
