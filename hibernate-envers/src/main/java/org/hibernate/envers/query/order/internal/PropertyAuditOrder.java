/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.order.internal;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.tools.Pair;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PropertyAuditOrder implements AuditOrder {
	private final PropertyNameGetter propertyNameGetter;
	private final boolean asc;

	public PropertyAuditOrder(PropertyNameGetter propertyNameGetter, boolean asc) {
		this.propertyNameGetter = propertyNameGetter;
		this.asc = asc;
	}

	@Override
	public Pair<String, Boolean> getData(EnversService enversService) {
		return Pair.make( propertyNameGetter.get( enversService ), asc );
	}
}
