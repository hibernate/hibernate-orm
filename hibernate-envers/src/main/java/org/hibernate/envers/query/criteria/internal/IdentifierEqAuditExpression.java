/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.boot.internal.EnversService;
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
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			String componentPrefix,
			QueryBuilder qb,
			Parameters parameters) {
		String prefix = enversService.getConfig().getOriginalIdPropertyName();
		enversService.getEntitiesConfigurations().get( entityName )
				.getIdMapper()
				.addIdEqualsToQuery( parameters, id, alias, prefix, equals );
	}
}
