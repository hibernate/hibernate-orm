/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.hbm;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.GenerationType;

import org.hibernate.tool.reveng.internal.util.TypeHelper;

/**
 * Static utility for resolving Hibernate type names to Java types,
 * class name qualification, and generator strategy mapping.
 *
 * @author Koen Aers
 */
class HbmTypeResolver {

	private static final Map<String, String> HIBERNATE_TYPE_MAP = new HashMap<>();

	static {
		HIBERNATE_TYPE_MAP.put("string", "java.lang.String");
		HIBERNATE_TYPE_MAP.put("long", "long");
		HIBERNATE_TYPE_MAP.put("int", "int");
		HIBERNATE_TYPE_MAP.put("integer", "java.lang.Integer");
		HIBERNATE_TYPE_MAP.put("short", "short");
		HIBERNATE_TYPE_MAP.put("byte", "byte");
		HIBERNATE_TYPE_MAP.put("float", "float");
		HIBERNATE_TYPE_MAP.put("double", "double");
		HIBERNATE_TYPE_MAP.put("boolean", "boolean");
		HIBERNATE_TYPE_MAP.put("yes_no", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("true_false", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("big_decimal", "java.math.BigDecimal");
		HIBERNATE_TYPE_MAP.put("big_integer", "java.math.BigInteger");
		HIBERNATE_TYPE_MAP.put("character", "java.lang.Character");
		HIBERNATE_TYPE_MAP.put("char", "char");
		HIBERNATE_TYPE_MAP.put("date", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("time", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("timestamp", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("calendar", "java.util.Calendar");
		HIBERNATE_TYPE_MAP.put("calendar_date", "java.util.Calendar");
		HIBERNATE_TYPE_MAP.put("binary", "byte[]");
		HIBERNATE_TYPE_MAP.put("byte[]", "byte[]");
		HIBERNATE_TYPE_MAP.put("text", "java.lang.String");
		HIBERNATE_TYPE_MAP.put("clob", "java.sql.Clob");
		HIBERNATE_TYPE_MAP.put("blob", "java.sql.Blob");
		HIBERNATE_TYPE_MAP.put("serializable", "java.io.Serializable");
	}

	static String resolveJavaType(String hibernateType) {
		if (hibernateType == null || hibernateType.isEmpty()) {
			return "java.lang.String";
		}
		String mapped = HIBERNATE_TYPE_MAP.get(hibernateType.toLowerCase());
		if (mapped != null) {
			return mapped;
		}
		if (hibernateType.contains(".")) {
			return hibernateType;
		}
		return "java.lang.String";
	}

	static boolean isPrimitiveType(String javaType) {
		return TypeHelper.isPrimitiveType(javaType);
	}

	static String resolveClassName(String name, String defaultPackage) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		if (name.contains(".")) {
			return name;
		}
		if (defaultPackage != null && !defaultPackage.isEmpty()) {
			return defaultPackage + "." + name;
		}
		return name;
	}

	static String simpleName(String fullName) {
		int lastDot = fullName.lastIndexOf('.');
		return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
	}

	static GenerationType mapGeneratorClass(String generatorClass) {
		if (generatorClass == null || generatorClass.isEmpty()) {
			return null;
		}
		return switch (generatorClass) {
			case "identity", "native" -> GenerationType.IDENTITY;
			case "sequence", "seqhilo",
				"enhanced-sequence", "org.hibernate.id.enhanced.SequenceStyleGenerator"
					-> GenerationType.SEQUENCE;
			case "enhanced-table", "org.hibernate.id.enhanced.TableGenerator"
					-> GenerationType.TABLE;
			case "uuid", "uuid2", "guid" -> GenerationType.UUID;
			case "assigned" -> null;
			default -> GenerationType.AUTO;
		};
	}

	private HbmTypeResolver() {}
}
