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
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * An audit query criterion that defines a predicate that is a comparison between a function
 * and an audit property expression.
 *
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class FunctionPropertyAuditExpression implements AuditCriterion {

	private String alias;
	private PropertyNameGetter propertyNameGetter;
	private AuditFunction function;
	private String op;

	public FunctionPropertyAuditExpression(
			String alias,
			PropertyNameGetter propertyNameGetter,
			AuditFunction function,
			String op) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
		this.function = function;
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
		String effectiveAlias = alias == null ? baseAlias : alias;
		String entityName = aliasToEntityNameMap.get( effectiveAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				entityName,
				propertyNameGetter
		);
		String propertyNamePrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				effectiveAlias
		);
		String prefixedPropertyName = propertyNamePrefix.concat( propertyName );
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, prefixedPropertyName );
		parameters.addWhereWithFunction(
				enversService.getConfig(),
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				effectiveAlias,
				prefixedPropertyName,
				op,
				function
		);
	}
}
