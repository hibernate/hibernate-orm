/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Locale;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NullAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;

	public NullAuditExpression(String alias, PropertyNameGetter propertyNameGetter) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
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
		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity( enversService, entityName, propertyName );
		String prefixedPropertyName = componentPrefix.concat( propertyName );

		if ( relatedEntity == null ) {
			parameters.addNullRestriction( alias, prefixedPropertyName );
		}
		else if ( relatedEntity.getRelationType() == RelationType.TO_ONE ) {
			relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, null, alias, null, true );
		}
		else {
			throw new AuditException(
					String.format(
							Locale.ENGLISH,
							"This type of relation (%s.%s) can't be used with null restrictions",
							entityName,
							propertyName
					)
			);
		}
	}
}
