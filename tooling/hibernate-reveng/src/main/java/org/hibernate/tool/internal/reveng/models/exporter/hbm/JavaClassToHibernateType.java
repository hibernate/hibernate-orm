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
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Java classes to Hibernate type name strings for use in hbm.xml generation.
 * This is the reverse of {@code HibernateTypeToJavaClass} and is used as a fallback
 * when {@code ColumnMetadata.getHibernateTypeName()} is not set.
 *
 * @author Koen Aers
 */
public class JavaClassToHibernateType {

	private static final Map<Class<?>, String> TYPE_MAP = new HashMap<>();

	static {
		// Primitive types
		TYPE_MAP.put(int.class, "int");
		TYPE_MAP.put(long.class, "long");
		TYPE_MAP.put(short.class, "short");
		TYPE_MAP.put(byte.class, "byte");
		TYPE_MAP.put(float.class, "float");
		TYPE_MAP.put(double.class, "double");
		TYPE_MAP.put(boolean.class, "boolean");
		TYPE_MAP.put(char.class, "character");

		// Wrapper types
		TYPE_MAP.put(Integer.class, "java.lang.Integer");
		TYPE_MAP.put(Long.class, "java.lang.Long");
		TYPE_MAP.put(Short.class, "java.lang.Short");
		TYPE_MAP.put(Byte.class, "java.lang.Byte");
		TYPE_MAP.put(Float.class, "java.lang.Float");
		TYPE_MAP.put(Double.class, "java.lang.Double");
		TYPE_MAP.put(Boolean.class, "java.lang.Boolean");
		TYPE_MAP.put(Character.class, "java.lang.Character");

		// String
		TYPE_MAP.put(String.class, "string");

		// Numeric types
		TYPE_MAP.put(BigDecimal.class, "big_decimal");
		TYPE_MAP.put(BigInteger.class, "big_integer");

		// Date/time
		TYPE_MAP.put(Date.class, "timestamp");

		// Binary
		TYPE_MAP.put(byte[].class, "binary");

		// Serializable fallback
		TYPE_MAP.put(java.io.Serializable.class, "serializable");
	}

	/**
	 * Returns the Hibernate type name for the given Java class.
	 *
	 * @param javaClass the Java class
	 * @return the Hibernate type name, or the class name if not found in the map
	 */
	public static String toHibernateType(Class<?> javaClass) {
		if (javaClass == null) {
			return "serializable";
		}
		String result = TYPE_MAP.get(javaClass);
		return result != null ? result : javaClass.getName();
	}
}
