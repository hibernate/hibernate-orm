/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
			AuditConfiguration auditCfg, AuditReaderImplementor versionsReader, String entityName,
			QueryBuilder qb, Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				auditCfg,
				versionsReader,
				entityName,
				propertyNameGetter
		);

		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity( auditCfg, entityName, propertyName );

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
