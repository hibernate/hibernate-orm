/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;

/**
 * @author Steve Ebersole
 */
public interface JdbcValueBindingsImplementor extends JdbcValueBindings {
	Object getBoundValue(String tableName, String columnName, ParameterUsage usage);

	void replaceValue(String tableName, String columnName, ParameterUsage usage, Object newValue);
}
