/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;
	private Object[] values;

	public InAuditExpression(String alias, PropertyNameGetter propertyNameGetter, Object[] values) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.values = values;
	}

	@Override
	protected void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			String componentPrefix,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);
		String prefixedPropertyName = componentPrefix.concat( propertyName );
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, prefixedPropertyName );
		parameters.addWhereWithParams( alias, prefixedPropertyName, "in (", values, ")" );
	}
}
