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

/**
 * A criterion that expresses that the id of an entity is equal or not equal to some specified value.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class IdentifierEqAuditExpression extends AbstractAtomicExpression {
	private final Object id;
	private final boolean equals;

	public IdentifierEqAuditExpression(String alias, Object id, boolean equals) {
		super( alias );
		this.id = id;
		this.equals = equals;
	}

	@Override
	protected void addToQuery(
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		final String prefix = versionsReader.getAuditService().getOptions().getOriginalIdPropName();
		versionsReader.getAuditService().getEntityBindings().get( entityName )
				.getIdMapper()
				.addIdEqualsToQuery( parameters, id, alias, prefix, equals );
	}
}
