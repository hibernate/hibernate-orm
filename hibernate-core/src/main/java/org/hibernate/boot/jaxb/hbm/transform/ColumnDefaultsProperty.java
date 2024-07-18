/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.mapping.Property;

/**
 * @author Steve Ebersole
 */
public class ColumnDefaultsProperty implements ColumnDefaults {
	private final Property property;

	public ColumnDefaultsProperty(Property property) {
		this.property = property;
	}

	@Override
	public Boolean isNullable() {
		return property.isOptional();
	}

	@Override
	public Boolean isInsertable() {
		return property.isInsertable();
	}

	@Override
	public Boolean isUpdateable() {
		return property.isUpdateable();
	}

	@Override
	public Integer getLength() {
		return null;
	}

	@Override
	public Integer getScale() {
		return null;
	}

	@Override
	public Integer getPrecision() {
		return null;
	}

	@Override
	public Boolean isUnique() {
		return Boolean.FALSE;
	}
}
