/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class LogicalAuditExpression implements AuditCriterion {
	private AuditCriterion lhs;
	private AuditCriterion rhs;
	private String op;

	public LogicalAuditExpression(AuditCriterion lhs, AuditCriterion rhs, String op) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.op = op;
	}

	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		Parameters opParameters = parameters.addSubParameters( op );

		lhs.addToQuery( enversService, versionsReader, aliasToEntityNameMap, alias, qb, opParameters.addSubParameters( "and" ) );
		rhs.addToQuery( enversService, versionsReader, aliasToEntityNameMap, alias, qb, opParameters.addSubParameters( "and" ) );
	}
}
