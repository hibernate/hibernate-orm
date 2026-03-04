/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.reader;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Hibernate type name strings (as returned by
 * {@code RevengStrategy.columnToHibernateTypeName()} or
 * {@code JdbcToHibernateTypeHelper.getPreferredHibernateType()})
 * to Java {@code Class<?>} objects needed by {@code ColumnMetadata}.
 *
 * @author Koen Aers
 */
public class HibernateTypeToJavaClass {

	private static final Map<String, Class<?>> TYPE_MAP = new HashMap<>();

	static {
		// Primitive types
		TYPE_MAP.put("int", int.class);
		TYPE_MAP.put("long", long.class);
		TYPE_MAP.put("short", short.class);
		TYPE_MAP.put("byte", byte.class);
		TYPE_MAP.put("float", float.class);
		TYPE_MAP.put("double", double.class);
		TYPE_MAP.put("boolean", boolean.class);
		TYPE_MAP.put("char", char.class);
		TYPE_MAP.put("character", char.class);

		// Wrapper types
		TYPE_MAP.put("java.lang.Integer", Integer.class);
		TYPE_MAP.put("java.lang.Long", Long.class);
		TYPE_MAP.put("java.lang.Short", Short.class);
		TYPE_MAP.put("java.lang.Byte", Byte.class);
		TYPE_MAP.put("java.lang.Float", Float.class);
		TYPE_MAP.put("java.lang.Double", Double.class);
		TYPE_MAP.put("java.lang.Boolean", Boolean.class);
		TYPE_MAP.put("java.lang.Character", Character.class);

		// String types
		TYPE_MAP.put("string", String.class);
		TYPE_MAP.put("java.lang.String", String.class);
		TYPE_MAP.put("clob", String.class);

		// Numeric types
		TYPE_MAP.put("big_decimal", BigDecimal.class);
		TYPE_MAP.put("java.math.BigDecimal", BigDecimal.class);
		TYPE_MAP.put("big_integer", BigInteger.class);
		TYPE_MAP.put("java.math.BigInteger", BigInteger.class);

		// Date/time types
		TYPE_MAP.put("date", Date.class);
		TYPE_MAP.put("time", Date.class);
		TYPE_MAP.put("timestamp", Date.class);
		TYPE_MAP.put("java.util.Date", Date.class);

		// Binary types
		TYPE_MAP.put("binary", byte[].class);
		TYPE_MAP.put("blob", byte[].class);

		// Serializable fallback
		TYPE_MAP.put("serializable", java.io.Serializable.class);
	}

	/**
	 * Converts a Hibernate type name to the corresponding Java class.
	 *
	 * @param hibernateTypeName the Hibernate type name (e.g. "string", "int", "big_decimal")
	 * @return the corresponding Java class, or {@code Object.class} if not recognized
	 */
	public static Class<?> toJavaClass(String hibernateTypeName) {
		if (hibernateTypeName == null) {
			return Object.class;
		}
		Class<?> result = TYPE_MAP.get(hibernateTypeName);
		return result != null ? result : Object.class;
	}
}
