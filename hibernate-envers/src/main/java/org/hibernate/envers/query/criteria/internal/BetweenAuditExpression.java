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
public class BetweenAuditExpression implements AuditCriterion {
	private PropertyNameGetter propertyNameGetter;
	private Object lo;
	private Object hi;

	public BetweenAuditExpression(PropertyNameGetter propertyNameGetter, Object lo, Object hi) {
		this.propertyNameGetter = propertyNameGetter;
		this.lo = lo;
		this.hi = hi;
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
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, propertyName );

		Parameters subParams = parameters.addSubParameters( Parameters.AND );
		subParams.addWhereWithParam( propertyName, ">=", lo );
		subParams.addWhereWithParam( propertyName, "<=", hi );
	}
}
