/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Locale;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

public class IlikeAuditExpression extends AbstractAtomicExpression {

	private PropertyNameGetter propertyNameGetter;
	private String value;

	public IlikeAuditExpression(String alias, PropertyNameGetter propertyNameGetter, String value) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.value = value;
	}

	@Override
	protected void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader, String entityName,
			String alias, QueryBuilder qb, Parameters parameters) {

		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, propertyName );

		parameters.addWhereWithFunction( alias, propertyName, " lower ", " like ", value.toLowerCase( Locale.ROOT ) );
	}

}
