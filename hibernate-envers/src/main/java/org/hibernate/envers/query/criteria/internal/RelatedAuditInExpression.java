/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Chris Cranford
 * @since 5.2
 */
public class RelatedAuditInExpression extends AbstractAtomicExpression {

	private final PropertyNameGetter propertyNameGetter;
	private final Object[] ids;

	public RelatedAuditInExpression(String alias, PropertyNameGetter propertyNameGetter, Object[] ids) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.ids = ids;
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
					"The criterion can only be used on a property that is a relation to another property." );
		}
		else if ( relatedEntity.getRelationType() != RelationType.TO_ONE ) {
			throw new AuditException(
					String.format(
							Locale.ENGLISH,
							"This type of relation (%s.%s) can't be used with related in restrictions",
							entityName,
							propertyName
					)
			);
		}

		// todo: should this throw an error if qpdList is null?  is it possible?
		List<QueryParameterData> qpdList = relatedEntity.getIdMapper().mapToQueryParametersFromId( propertyName );
		if ( qpdList != null ) {
			QueryParameterData qpd = qpdList.iterator().next();
			parameters.addWhereWithParams( alias, componentPrefix.concat( qpd.getQueryParameterName() ), "in (", ids, ")" );
		}
	}
}
