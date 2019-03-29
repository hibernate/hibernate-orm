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
import org.hibernate.sql.ast.produce.internal.SqlAstDeleteDescriptorImpl;
import org.hibernate.sql.ast.produce.sqm.spi.SqmDeleteInterpretation;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteInterpretationImpl extends SqlAstDeleteDescriptorImpl implements SqmDeleteInterpretation {
	private final Map<SqmParameter, List<JdbcParameter>> jdbcParamXref;

	public SqmDeleteInterpretationImpl(
			DeleteStatement sqlAstStatement,
			Set<String> affectedTables,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamXref) {
		super( sqlAstStatement, affectedTables );
		this.jdbcParamXref = jdbcParamXref;
	}

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamXref;
	}
}
