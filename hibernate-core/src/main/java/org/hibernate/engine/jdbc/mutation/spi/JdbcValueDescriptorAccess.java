/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * @author Steve Ebersole
 */
public interface JdbcValueDescriptorAccess {
	default String resolvePhysicalTableName(String tableName) {
		return tableName;
	}

	JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage);
}
