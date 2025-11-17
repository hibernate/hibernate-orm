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
public class BetweenAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;
	private Object lo;
	private Object hi;

	public BetweenAuditExpression(String alias, PropertyNameGetter propertyNameGetter, Object lo, Object hi) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.lo = lo;
		this.hi = hi;
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

		Parameters subParams = parameters.addSubParameters( Parameters.AND );
		subParams.addWhereWithParam( alias, prefixedPropertyName, ">=", lo );
		subParams.addWhereWithParam( alias, prefixedPropertyName, "<=", hi );
	}
}
