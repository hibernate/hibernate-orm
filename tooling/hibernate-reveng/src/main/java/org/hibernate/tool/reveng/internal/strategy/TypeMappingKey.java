/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

class TypeMappingKey {

	final int type;
	final int length;

	TypeMappingKey(SQLTypeMapping mpa) {
		type = mpa.getJDBCType();
		length = mpa.getLength();
	}

	TypeMappingKey(int sqlType, int length) {
		this.type = sqlType;
		this.length = length;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof TypeMappingKey other)) return false;
		return type == other.type && length == other.length;
	}

	@Override
	public int hashCode() {
		return (type + length) % 17;
	}

	@Override
	public String toString() {
		return this.getClass() + "(type:" + type + ", length:" + length + ")";
	}
}
