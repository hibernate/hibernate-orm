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

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionTypeAuditExpression extends AbstractAtomicExpression {
	private Object value;
	private String op;

	public RevisionTypeAuditExpression(String alias, Object value, String op) {
		super( alias );
		this.value = value;
		this.op = op;
	}

	@Override
	protected void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		parameters.addWhereWithParam( alias, enversService.getAuditEntitiesConfiguration().getRevisionTypePropName(), op, value );
	}
}
