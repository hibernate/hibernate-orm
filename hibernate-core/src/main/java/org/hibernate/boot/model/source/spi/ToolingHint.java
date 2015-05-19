/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
