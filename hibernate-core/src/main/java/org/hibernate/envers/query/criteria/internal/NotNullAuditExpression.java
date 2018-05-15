/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class NotNullAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;

	public NotNullAuditExpression(String alias, PropertyNameGetter propertyNameGetter) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
	}

	@Override
	protected void addToQuery(
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				versionsReader,
				entityName,
				propertyNameGetter
		);
		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity(
				versionsReader.getAuditService(),
				entityName,
				propertyName
		);
		if ( relatedEntity == null ) {
			parameters.addNotNullRestriction( alias, propertyName );
		}
		else {
			relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, null, alias, null, false );
		}
	}
}
