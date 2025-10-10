/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayJavaType;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR} and {@code char[]}.
 *
 * @author Strong Liu
 */
public class PrimitiveCharacterArrayTextType extends AbstractSingleColumnStandardBasicType<char[]> {
	public static final PrimitiveCharacterArrayTextType INSTANCE = new PrimitiveCharacterArrayTextType();

	public PrimitiveCharacterArrayTextType() {
		super( LongVarcharJdbcType.INSTANCE, PrimitiveCharacterArrayJavaType.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
