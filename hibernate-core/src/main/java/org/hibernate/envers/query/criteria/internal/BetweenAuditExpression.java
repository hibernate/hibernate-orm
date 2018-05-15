/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class BetweenAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;
	private Object lo;
	private Object hi;

	public BetweenAuditExpression(String alias, PropertyNameGetter propertyNameGetter, Object lo, Object hi) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.lo = lo;
		this.hi = hi;
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
		CriteriaTools.checkPropertyNotARelation( versionsReader.getAuditService(), entityName, propertyName );

		Parameters subParams = parameters.addSubParameters( Parameters.AND );
		subParams.addWhereWithParam( alias, propertyName, ">=", lo );
		subParams.addWhereWithParam( alias, propertyName, "<=", hi );
	}
}
