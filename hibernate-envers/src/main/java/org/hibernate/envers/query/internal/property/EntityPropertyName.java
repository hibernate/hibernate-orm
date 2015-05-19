/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.boot.internal.EnversService;

/**
 * Used for specifying restrictions on a property of an audited entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityPropertyName implements PropertyNameGetter {
	private final String propertyName;

	public EntityPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String get(EnversService enversService) {
		return propertyName;
	}
}
