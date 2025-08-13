/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * @author Steve Ebersole
 */
public class JdbcOperationQueryUpdate
		extends AbstractJdbcOperationQuery
		implements JdbcOperationQueryMutation {
	public JdbcOperationQueryUpdate(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters) {
		super( sql, parameterBinders, affectedTableNames, appliedParameters );
	}
}
