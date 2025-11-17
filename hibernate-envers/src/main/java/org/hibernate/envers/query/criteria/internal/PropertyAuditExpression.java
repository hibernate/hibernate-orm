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
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PropertyAuditExpression implements AuditCriterion {
	private String alias;
	private PropertyNameGetter propertyNameGetter;
	private String otherAlias;
	private String otherPropertyName;
	private String op;

	public PropertyAuditExpression(
			String alias,
			PropertyNameGetter propertyNameGetter,
			String otherAlias,
			String otherPropertyName,
			String op
	) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
		this.otherAlias = otherAlias;
		this.otherPropertyName = otherPropertyName;
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
		String effectiveAlias = alias == null ? baseAlias : alias;
		String effectiveOtherAlias = otherAlias == null ? baseAlias : otherAlias;
		String entityName = aliasToEntityNameMap.get( effectiveAlias );
		String otherEntityName = aliasToEntityNameMap.get( effectiveOtherAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);
		String propertyNamePrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				effectiveAlias
		);
		String otherPropertyNamePrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				effectiveOtherAlias
		);
		String prefixedPropertyName = propertyNamePrefix.concat( propertyName );
		String prefixedOtherPropertyName = otherPropertyNamePrefix.concat( otherPropertyName );
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, prefixedPropertyName );
		/*
		 * Check that the other property name is not a relation. However, we can only
		 * do this for audited entities. If the other property belongs to a non-audited
		 * entity, we have to skip this check.
		 */
		if ( enversService.getEntitiesConfigurations().isVersioned( otherEntityName ) ) {
			CriteriaTools.checkPropertyNotARelation( enversService, otherEntityName, prefixedOtherPropertyName );
		}
		parameters.addWhere( effectiveAlias, prefixedPropertyName, op, effectiveOtherAlias, prefixedOtherPropertyName );
	}
}
