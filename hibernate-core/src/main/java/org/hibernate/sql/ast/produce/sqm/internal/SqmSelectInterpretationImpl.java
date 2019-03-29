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
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectInterpretation;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.results.spi.DomainResult;

/**
 * The standard Hibernate implementation of SqlAstSelectDescriptor.
 *
 * @author Steve Ebersole
 */
public class SqmSelectInterpretationImpl extends SqlAstSelectDescriptorImpl implements SqmSelectInterpretation {
	private final Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam;

	public SqmSelectInterpretationImpl(
			SelectStatement selectQuery,
			List<DomainResult> queryReturns,
			Set<String> affectedTables,
			Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam) {
		super( selectQuery, queryReturns, affectedTables );
		this.jdbcParamsBySqmParam = jdbcParamsBySqmParam;
	}

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}
}
