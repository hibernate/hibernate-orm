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
public class SimpleAuditExpression implements AuditCriterion {
	private PropertyNameGetter propertyNameGetter;
	private Object value;
	private String op;

	public SimpleAuditExpression(PropertyNameGetter propertyNameGetter, Object value, String op) {
		this.propertyNameGetter = propertyNameGetter;
		this.value = value;
		this.op = op;
	}

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
			parameters.addWhereWithParam( propertyName, op, value );
		}
		else {
			if ( !"=".equals( op ) && !"<>".equals( op ) ) {
				throw new AuditException(
						"This type of operation: " + op + " (" + entityName + "." + propertyName +
								") isn't supported and can't be used in queries."
				);
			}

			Object id = relatedEntity.getIdMapper().mapToIdFromEntity( value );

			relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, id, null, "=".equals( op ) );
		}
	}
}
