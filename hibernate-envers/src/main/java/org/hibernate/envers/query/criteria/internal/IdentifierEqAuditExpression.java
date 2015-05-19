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

/**
 * A criterion that expresses that the id of an entity is equal or not equal to some specified value.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class IdentifierEqAuditExpression implements AuditCriterion {
	private final Object id;
	private final boolean equals;

	public IdentifierEqAuditExpression(Object id, boolean equals) {
		this.id = id;
		this.equals = equals;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			QueryBuilder qb,
			Parameters parameters) {
		enversService.getEntitiesConfigurations().get( entityName )
				.getIdMapper()
				.addIdEqualsToQuery( parameters, id, enversService.getAuditEntitiesConfiguration().getOriginalIdPropName(), equals );
	}
}
