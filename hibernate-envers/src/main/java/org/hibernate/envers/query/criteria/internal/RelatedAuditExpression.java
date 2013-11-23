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
