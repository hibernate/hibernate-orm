/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RelatedAuditExpression implements AuditCriterion {
	private final PropertyNameGetter propertyNameGetter;
	private final Object id;
	private final boolean equals;

	public RelatedAuditExpression(PropertyNameGetter propertyNameGetter, Object id, boolean equals) {
		this.propertyNameGetter = propertyNameGetter;
		this.id = id;
		this.equals = equals;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);

		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity( enversService, entityName, propertyName );

		if ( relatedEntity == null ) {
			throw new AuditException(
					"This criterion can only be used on a property that is " +
							"a relation to another property."
			);
		}
		else {
			relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, id, null, equals );
		}
	}
}
