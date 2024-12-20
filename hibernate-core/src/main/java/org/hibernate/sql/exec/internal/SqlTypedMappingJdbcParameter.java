/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

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

	public SqlTypedMapping getSqlTypedMapping() {
		return sqlTypedMapping;
	}
}
