/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * A BasicType adapter targeting partial portability to 6.0's type
 * system changes.  In 6.0 the notion of a BasicType is just a
 * combination of JavaType/JdbcType.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class StandardBasicTypeTemplate<J> extends AbstractSingleColumnStandardBasicType<J> {
	private final String name;
	private final String[] registrationKeys;

	public StandardBasicTypeTemplate(
			JdbcType jdbcType,
			JavaType<J> javaType,
			String... registrationKeys) {
		super( jdbcType, javaType );
		this.registrationKeys = registrationKeys;

		this.name = javaType.getJavaType() == null ? "(map-mode)" : javaType.getTypeName()
				+ " -> " + jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registrationKeys;
	}
}
