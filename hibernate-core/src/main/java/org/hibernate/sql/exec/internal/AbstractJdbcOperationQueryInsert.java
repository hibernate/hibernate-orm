/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.sql.exec.spi.AbstractJdbcOperationQuery;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * Base support for JdbcInsertMutation implementations
 *
 * @author Steve Ebersole
 */
public class AbstractJdbcOperationQueryInsert extends AbstractJdbcOperationQuery implements JdbcOperationQueryInsert {

	private final String uniqueConstraintNameThatMayFail;

	public AbstractJdbcOperationQueryInsert(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames,
			String uniqueConstraintNameThatMayFail) {
		super( sql, parameterBinders, affectedTableNames );
		this.uniqueConstraintNameThatMayFail = uniqueConstraintNameThatMayFail;
	}

	@Override
	public String getUniqueConstraintNameThatMayFail() {
		return uniqueConstraintNameThatMayFail;
	}
}
