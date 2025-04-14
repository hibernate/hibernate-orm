/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.SqlTypedMapping;

/**
 * @author Steve Ebersole
 */
public class SqlTypedMappingJdbcParameter extends AbstractJdbcParameter {

	private final SqlTypedMapping sqlTypedMapping;

	public SqlTypedMappingJdbcParameter(SqlTypedMapping sqlTypedMapping) {
		super( sqlTypedMapping.getJdbcMapping() );
		this.sqlTypedMapping = sqlTypedMapping;
	}

	public SqlTypedMappingJdbcParameter(SqlTypedMapping sqlTypedMapping, @Nullable Integer parameterId) {
		super( sqlTypedMapping.getJdbcMapping(), parameterId );
		this.sqlTypedMapping = sqlTypedMapping;
	}

	public SqlTypedMapping getSqlTypedMapping() {
		return sqlTypedMapping;
	}
}
