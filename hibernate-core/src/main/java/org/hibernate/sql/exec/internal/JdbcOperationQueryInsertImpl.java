/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.exec.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

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
}
