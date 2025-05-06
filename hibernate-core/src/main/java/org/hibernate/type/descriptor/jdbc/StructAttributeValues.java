/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.metamodel.spi.ValueAccess;

/**
 * @author Marco Belladelli
 */
public class StructAttributeValues implements ValueAccess {
	private final Object[] attributeValues;
	private final int size;
	private Object discriminator;

	public StructAttributeValues(int size, Object[] rawJdbcValues) {
		this.size = size;
		if ( rawJdbcValues == null || size != rawJdbcValues.length) {
			attributeValues = new Object[size];
		}
		else {
			attributeValues = rawJdbcValues;
		}
	}

	@Override
	public Object[] getValues() {
		return attributeValues;
	}

	public void setAttributeValue(int index, Object value) {
		if ( index == size ) {
			discriminator = value;
		}
		else {
			attributeValues[index] = value;
		}
	}

	public Object getDiscriminator() {
		return discriminator;
	}
}
