/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * @author Steve Ebersole
 */
public class JdbcValueDescriptorImpl implements JdbcValueDescriptor {
	private final String normalizedColumnName;
	private final JdbcMapping jdbcMapping;
	private final ParameterUsage parameterUsage;
	private final int parameterIndex;

	public JdbcValueDescriptorImpl(
			String normalizedColumnName,
			JdbcMapping jdbcMapping,
			ParameterUsage parameterUsage,
			int parameterIndex) {
		this.normalizedColumnName = normalizedColumnName;
		this.jdbcMapping = jdbcMapping;
		this.parameterUsage = parameterUsage;
		this.parameterIndex = parameterIndex;
	}

	@Override
	public String getColumnName() {
		return normalizedColumnName;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public ParameterUsage getUsage() {
		return parameterUsage;
	}

	@Override
	public int getJdbcPosition() {
		return parameterIndex;
	}
}
