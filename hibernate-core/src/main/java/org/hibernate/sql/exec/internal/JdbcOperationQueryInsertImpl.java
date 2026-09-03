/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * Standard insert operation
 *
 * @author Steve Ebersole
 */
public class JdbcOperationQueryInsertImpl
		extends AbstractJdbcOperationQueryInsert
		implements JdbcOperationQueryMutation {


	public JdbcOperationQueryInsertImpl(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames) {
		super( sql, parameterBinders, affectedTableNames, null );
	}

	public JdbcOperationQueryInsertImpl(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames,
			String uniqueConstraintNameThatMayFail) {
		super( sql, parameterBinders, affectedTableNames, uniqueConstraintNameThatMayFail );
	}

	public JdbcOperationQueryInsertImpl(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters,
			String uniqueConstraintNameThatMayFail) {
		super( sql, parameterBinders, affectedTableNames, appliedParameters, uniqueConstraintNameThatMayFail );
	}
}
