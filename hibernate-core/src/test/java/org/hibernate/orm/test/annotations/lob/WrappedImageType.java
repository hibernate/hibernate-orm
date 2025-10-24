/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcType;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY} and {@code Byte[]}
 *
 * @author Strong Liu
 */
public class WrappedImageType extends AbstractSingleColumnStandardBasicType<Byte[]> {
	public static final WrappedImageType INSTANCE = new WrappedImageType();

	public WrappedImageType() {
		super( LongVarbinaryJdbcType.INSTANCE, ByteArrayJavaType.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
