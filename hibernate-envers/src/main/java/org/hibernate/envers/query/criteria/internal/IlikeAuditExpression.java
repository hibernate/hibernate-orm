/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Locale;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

public class IlikeAuditExpression extends AbstractAtomicExpression {

	private PropertyNameGetter propertyNameGetter;
	private String value;

	public IlikeAuditExpression(String alias, PropertyNameGetter propertyNameGetter, String value) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.value = value;
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
		parameters.addWhereWithFunction( alias, prefixedPropertyName, " lower ", " like ", value.toLowerCase( Locale.ROOT ) );
	}

}
