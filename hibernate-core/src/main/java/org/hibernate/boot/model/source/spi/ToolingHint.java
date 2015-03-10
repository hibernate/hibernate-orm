/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.mapping.MetaAttribute;

/**
 * @author Steve Ebersole
 */
public class ToolingHint {
	private final String name;
	private final boolean inheritable;
	private final MetaAttribute metaAttribute;

	public ToolingHint(String name, boolean inheritable) {
		this.name = name;
		this.inheritable = inheritable;

		this.metaAttribute = new MetaAttribute( name );
	}

	public String getName() {
		return name;
	}

	public boolean isInheritable() {
		return inheritable;
	}

	public java.util.List getValues() {
		return metaAttribute.getValues();
	}

	public void addValue(String value) {
		metaAttribute.addValue( value );
	}

	public String getValue() {
		return metaAttribute.getValue();
	}

	public boolean isMultiValued() {
		return metaAttribute.isMultiValued();
	}

	@Override
	public String toString() {
		return "ToolingHint{" +
				"name='" + name + '\'' +
				", inheritable=" + inheritable +
				", values=" + metaAttribute.getValues() +
				'}';
	}

	public MetaAttribute asMetaAttribute() {
		return metaAttribute;
	}
}
