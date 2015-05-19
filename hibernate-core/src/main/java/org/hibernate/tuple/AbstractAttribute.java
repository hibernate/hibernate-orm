/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttribute implements Attribute, Property {
	private final String attributeName;
	private final Type attributeType;

	protected AbstractAttribute(String attributeName, Type attributeType) {
		this.attributeName = attributeName;
		this.attributeType = attributeType;
	}

	@Override
	public String getNode() {
		return null;
	}

	@Override
	public String getName() {
		return attributeName;
	}

	@Override
	public Type getType() {
		return attributeType;
	}
}
