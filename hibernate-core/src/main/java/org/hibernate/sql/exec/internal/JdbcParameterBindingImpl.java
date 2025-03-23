/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.Locale;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * @author Andrea Boriero
 */
public class JdbcParameterBindingImpl implements JdbcParameterBinding {
	private final JdbcMapping jdbcMapping;
	private final Object bindValue;

	public JdbcParameterBindingImpl(JdbcMapping jdbcMapping, Object bindValue) {
		assert bindValue == null || jdbcMapping == null || jdbcMapping.getJdbcJavaType().isInstance( bindValue )
				: String.format( Locale.ROOT, "Unexpected value type (expected : %s) : %s (%s)",
						jdbcMapping.getJdbcJavaType().getJavaTypeClass().getName(), bindValue, bindValue.getClass().getName() );

		this.jdbcMapping = jdbcMapping;
		this.bindValue = bindValue;
	}

	@Override
	public JdbcMapping getBindType() {
		return jdbcMapping;
	}

	@Override
	public Object getBindValue() {
		return bindValue;
	}
}
