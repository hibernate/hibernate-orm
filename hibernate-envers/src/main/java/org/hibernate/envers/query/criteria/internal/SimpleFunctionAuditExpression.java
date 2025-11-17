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
 * An audit query criterion that compares a function call with a scalar value.
 *
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class SimpleFunctionAuditExpression implements AuditCriterion {

	private AuditFunction function;
	private Object value;
	private String op;

	public SimpleFunctionAuditExpression(AuditFunction function, Object value, String op) {
		this.function = function;
		this.value = value;
		this.op = op;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder qb,
			Parameters parameters) {
		parameters.addWhereWithFunction(
				enversService.getConfig(),
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				function,
				op,
				value
		);
	}

}
