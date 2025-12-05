/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.tree.expression.SqlTypedExpression;

/**
 * @author Steve Ebersole
 */
public class SqlTypedMappingJdbcParameter extends AbstractJdbcParameter implements SqlTypedExpression {

	private final SqlTypedMapping sqlTypedMapping;

	public SqlTypedMappingJdbcParameter(SqlTypedMapping sqlTypedMapping) {
		super( sqlTypedMapping.getJdbcMapping() );
		this.sqlTypedMapping = sqlTypedMapping;
	}

	public SqlTypedMappingJdbcParameter(SqlTypedMapping sqlTypedMapping, @Nullable Integer parameterId) {
		super( sqlTypedMapping.getJdbcMapping(), parameterId );
		this.sqlTypedMapping = sqlTypedMapping;
	}

	@Override
	public SqlTypedMapping getSqlTypedMapping() {
		return sqlTypedMapping;
	}
}
