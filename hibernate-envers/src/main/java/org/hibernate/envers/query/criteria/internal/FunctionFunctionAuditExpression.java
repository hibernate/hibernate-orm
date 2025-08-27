/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
 * An audit query criterion that defines a predicate where both sides are a function.
 *
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
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder queryBuilder,
			Parameters parameters) {
		parameters.addWhereWithFunction(
				enversService.getConfig(),
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				leftFunction,
				op,
				rightFunction
		);
	}
}
