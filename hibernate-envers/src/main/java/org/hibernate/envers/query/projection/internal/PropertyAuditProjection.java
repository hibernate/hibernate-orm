/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.projection.internal;

import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class PropertyAuditProjection implements AuditProjection {
	private final String alias;
	private final PropertyNameGetter propertyNameGetter;
	private final String function;
	private final boolean distinct;

	public PropertyAuditProjection(String alias, PropertyNameGetter propertyNameGetter, String function, boolean distinct) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
		this.function = function;
		this.distinct = distinct;
	}

	@Override
	public String getAlias(String baseAlias) {
		return alias == null ? baseAlias : alias;
	}

	@Override
	public void addProjectionToQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder queryBuilder) {
		String projectionEntityAlias = getAlias( baseAlias );
		String projectionEntityName = aliasToEntityNameMap.get( projectionEntityAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				projectionEntityName,
				propertyNameGetter
		);
		String propertyNamePrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				projectionEntityAlias
		);
		queryBuilder.addProjection(
				function,
				projectionEntityAlias,
				propertyNamePrefix.concat( propertyName ),
				distinct
		);
	}

	@Override
	public Object convertQueryResult(
			EnversService enversService,
			EntityInstantiator entityInstantiator,
			String entityName,
			Number revision,
			Object value) {
		return value;
	}
}
