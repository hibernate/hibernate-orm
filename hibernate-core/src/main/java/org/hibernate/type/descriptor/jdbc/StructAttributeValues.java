package org.hibernate.type.descriptor.jdbc;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.metamodel.spi.ValueAccess;

/**
 * @author Marco Belladelli
 */
public class StructAttributeValues implements ValueAccess {
	private final Object[] attributeValues;
	private final int size;
	private Object discriminator;

	public StructAttributeValues(int size, @Nullable Object[] rawJdbcValues) {
		this.size = size;
		if ( rawJdbcValues == null || size != rawJdbcValues.length) {
			attributeValues = new Object[size];
		}
		else {
			attributeValues = rawJdbcValues;
		}
	}

	@Override
	@Nonnull
	public Object[] getValues() {
		return attributeValues;
	}

	public void setAttributeValue(int index, @Nullable Object value) {
		if ( index == size ) {
			discriminator = value;
		}
		else {
			attributeValues[index] = value;
		}
	}

	@Nullable
	public Object getDiscriminator() {
		return discriminator;
	}
}
