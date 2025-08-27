/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR} and {@code Character[]}.
 *
 * @author Strong Liu
 */
public class CharacterArrayTextType extends AbstractSingleColumnStandardBasicType<Character[]> {
	public static final CharacterArrayTextType INSTANCE = new CharacterArrayTextType();

	public CharacterArrayTextType() {
		super( LongVarcharJdbcType.INSTANCE, CharacterArrayJavaType.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
