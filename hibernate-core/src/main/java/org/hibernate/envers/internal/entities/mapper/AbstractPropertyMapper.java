/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

/**
 * Abstract implementation of a {@link PropertyMapper}.
 *
 * @author Chris Cranford
 */
public abstract class AbstractPropertyMapper implements PropertyMapper {
	private boolean map;

	@Override
	public void markAsDynamicComponentMap() {
		this.map = true;
	}

	@Override
	public boolean isDynamicComponentMap() {
		return map;
	}
}
