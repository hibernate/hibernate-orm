/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
