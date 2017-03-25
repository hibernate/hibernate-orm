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
					"This criterion can only be used on a property that is a relation to another property."
			);
		}
		relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, id, alias, null, equals );
	}
}
