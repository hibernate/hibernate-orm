/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateInterpretation;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateInterpretationImpl implements SqmUpdateInterpretation {
	private final List<UpdateStatement> sqlUpdates;
	private final Set<String> affectedTables;
	private final Map<SqmParameter, List<JdbcParameter>> jdbcParamXref;

	public SqmUpdateInterpretationImpl(
			List<UpdateStatement> sqlUpdates,
			Set<String> affectedTables,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamXref) {
		this.sqlUpdates = sqlUpdates;
		this.affectedTables = affectedTables;
		this.jdbcParamXref = jdbcParamXref;
	}

	public List<UpdateStatement> getSqlUpdates() {
		return sqlUpdates;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTables;
	}

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamXref;
	}
}
