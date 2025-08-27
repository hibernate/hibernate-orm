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
 * @author Chris Cranford
 * @since 5.2
 */
public class RelatedAuditEqualityExpression extends AbstractAtomicExpression {
	private final PropertyNameGetter propertyNameGetter;
	private final Object id;
	private final boolean equals;

	public RelatedAuditEqualityExpression(String alias, PropertyNameGetter propertyNameGetter, Object id, boolean equals) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.id = id;
		this.equals = equals;
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

		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity(
				enversService,
				entityName,
				componentPrefix.concat( propertyName )
		);

		if ( relatedEntity == null ) {
			throw new AuditException(
					"This criterion can only be used on a property that is a relation to another property." );
		}
		else if ( relatedEntity.getRelationType() != RelationType.TO_ONE ) {
			throw new AuditException(
					String.format(
							Locale.ENGLISH,
							"This type of relation (%s.%s) can't be used with related equality restrictions",
							entityName,
							propertyName
					)
			);
		}
		relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, id, alias, null, equals );
	}
}
