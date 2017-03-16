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
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
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
			String baseAlias,
			QueryBuilder queryBuilder,
			Parameters parameters) {
		String effectiveAlias = alias == null ? baseAlias : alias;
		String entityName = aliasToEntityNameMap.get( effectiveAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				entityName,
				propertyNameGetter );
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, propertyName );
		parameters.addWhereWithFunction( enversService, effectiveAlias, propertyName, op, function );
	}
}
