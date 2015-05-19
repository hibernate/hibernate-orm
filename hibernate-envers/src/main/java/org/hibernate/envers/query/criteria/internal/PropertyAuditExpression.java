/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PropertyAuditExpression implements AuditCriterion {
	private PropertyNameGetter propertyNameGetter;
	private String otherPropertyName;
	private String op;

	public PropertyAuditExpression(PropertyNameGetter propertyNameGetter, String otherPropertyName, String op) {
		this.propertyNameGetter = propertyNameGetter;
		this.otherPropertyName = otherPropertyName;
		this.op = op;
	}

	public void addToQuery(
			EnversService enversService, AuditReaderImplementor versionsReader,
			String entityName,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, propertyName );
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, otherPropertyName );
		parameters.addWhere( propertyName, op, otherPropertyName );
	}
}
