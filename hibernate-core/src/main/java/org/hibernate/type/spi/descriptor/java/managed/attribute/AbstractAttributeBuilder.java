/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed.attribute;

import javax.persistence.metamodel.Attribute;

import org.hibernate.type.spi.descriptor.java.managed.AttributeBuilder;
import org.hibernate.type.spi.descriptor.java.managed.AttributeDeclarer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBuilder<T extends Attribute> implements AttributeBuilder<T> {
	private final AttributeDeclarer attributeDeclarer;
	private final String attributeName;

	private T builtAttribute;

	public AbstractAttributeBuilder(AttributeDeclarer attributeDeclarer, String attributeName) {
		this.attributeDeclarer = attributeDeclarer;
		this.attributeName = attributeName;
	}

	@Override
	public String getAttributeName() {
		return attributeName;
	}

	@Override
	public T build() {
		if ( builtAttribute == null ) {
			builtAttribute = generateAttribute();
			attributeDeclarer.attributeBuilt( builtAttribute );
		}

		return builtAttribute;
	}

	protected abstract T generateAttribute();
}
