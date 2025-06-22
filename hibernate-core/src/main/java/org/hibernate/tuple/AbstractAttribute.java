/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.type.Type;

/**
 * @deprecated No direct replacement.
 */
@Deprecated(forRemoval = true)
public abstract class AbstractAttribute implements Attribute {
	private final String attributeName;
	private final Type attributeType;

	protected AbstractAttribute(String attributeName, Type attributeType) {
		this.attributeName = attributeName;
		this.attributeType = attributeType;
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
