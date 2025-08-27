/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.Objects;

/**
 * Models a JDBC {@linkplain java.sql.Types data type}.  Mainly breaks down into 3 pieces of information:<ul>
 *     <li>
 *         {@link #getTypeCode() type code} - The JDBC type code; generally matches a code from {@link java.sql.Types}
 *         though not necessarily.
 *     </li>
 *     <li>
 *         {@link #getTypeName() type name} - The database type name for the given type code.
 *     </li>
 *     <li>
 *         {@link #getJavaType()} java type} - The java type recommended for representing this JDBC type (if known)
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class JdbcDataType {
	private final int typeCode;
	private final String typeName;
	private final Class<?> javaType;
	private final int hashCode; // not a record type because we want to cache this

	public JdbcDataType(int typeCode, String typeName, Class<?> javaType) {
		this.typeCode = typeCode;
		this.typeName = typeName;
		this.javaType = javaType;
		this.hashCode = Objects.hash( typeCode, typeName, javaType );
	}

	public int getTypeCode() {
		return typeCode;
	}

	public String getTypeName() {
		return typeName;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JdbcDataType jdbcDataType) ) {
			return false;
		}
		return typeCode == jdbcDataType.typeCode
			&& javaType.equals( jdbcDataType.javaType )
			&& typeName.equals( jdbcDataType.typeName );
	}

	@Override
	public String toString() {
		return super.toString() + "[code=" + typeCode + ", name=" + typeName + ", javaClass=" + javaType.getName() + "]";
	}
}
