/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.projection.internal;

import java.util.Map;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.metamodel.spi.EntityInstantiator;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 * @author Chris Cranford
 */
public class EntityAuditProjection implements AuditProjection {

	private final String alias;
	private final boolean distinct;

	public EntityAuditProjection(String alias, boolean distinct) {
		this.alias = alias;
		this.distinct = distinct;
	}

	@Override
	public ProjectionData getData(AuditService auditService) {
		// no property is selected, instead the whole entity (alias) is selected
		return new ProjectionData( null, alias, null, distinct );
	}

	@Override
	public Object convertQueryResult(
			EnversService enversService,
			EntityInstantiator entityInstantiator,
			String entityName,
			Number revision,
			Object value) {
		final Object result;
		if ( entityInstantiator.getAuditService().getEntityBindings().isVersioned( entityName ) ) {
			result = entityInstantiator.createInstanceFromVersionsEntity( entityName, (Map) value, revision );
		}
		else {
			result = value;
		}
		return result;
	}

}
