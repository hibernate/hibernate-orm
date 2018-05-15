/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.projection.internal;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class PropertyAuditProjection implements AuditProjection {
	private final String alias;
	private final PropertyNameGetter propertyNameGetter;
	private final String function;
	private final boolean distinct;

	public PropertyAuditProjection(String alias, PropertyNameGetter propertyNameGetter, String function, boolean distinct) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
		this.function = function;
		this.distinct = distinct;
	}

	@Override
	public ProjectionData getData(AuditService auditService) {
		final String propertyName = propertyNameGetter.get( auditService );
		return new ProjectionData( function, alias, propertyName, distinct );
	}

	@Override
	public Object convertQueryResult(EntityInstantiator entityInstantiator, String entityName, Number revision, Object value) {
		return value;
	}
}
