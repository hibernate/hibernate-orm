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
import org.hibernate.envers.query.criteria.AuditFunction;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class FunctionFunctionAuditExpression implements AuditCriterion {

	private AuditFunction leftFunction;
	private AuditFunction rightFunction;
	private String op;

	public FunctionFunctionAuditExpression(
			AuditFunction leftFunction,
			AuditFunction rightFunction,
			String op) {
		this.leftFunction = leftFunction;
		this.rightFunction = rightFunction;
		this.op = op;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap,
			String baseAlias,
			QueryBuilder queryBuilder,
			Parameters parameters) {
		parameters.addWhereWithFunction( enversService, leftFunction, op, rightFunction );
	}
}
