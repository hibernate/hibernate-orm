/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class PrimitiveBasicTypeImpl<J> extends BasicTypeImpl<J> {

	public PrimitiveBasicTypeImpl(JavaType<J> jtd, JdbcType std) {
		super( jtd, std );
	}

	@Override
	@Nonnull
	public Class<J> getJavaType() {
		//noinspection unchecked
		return (Class<J>) ((PrimitiveJavaType<?>) getJavaTypeDescriptor()).getPrimitiveClass();
	}

}
