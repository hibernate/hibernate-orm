/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.TemporalType;

import org.hibernate.MappingException;

/**
 * Consolidated utility for type conversions used during reverse engineering:
 * <ul>
 *   <li>JDBC SQL type &harr; Hibernate type name</li>
 *   <li>Hibernate type name &rarr; Java class</li>
 *   <li>Java class name &rarr; JDBC SQL type</li>
 *   <li>JDBC type name &harr; integer constant</li>
 *   <li>SQL type metadata (has scale/precision/length)</li>
 * </ul>
 *
 * @author max (original JdbcToHibernateTypeHelper)
 * @author koen
 */
public final class TypeHelper {

	private TypeHelper() {
	}

	// ========================================================================
	// JDBC SQL type -> Hibernate type name
	// ========================================================================

	/** Maps JDBC SQL type constants to [primitive, nullable] Hibernate type name pairs. */
	private static final Map<Integer, String[]> PREFERRED_HIBERNATETYPE_FOR_SQLTYPE = new HashMap<>();

	static {
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.TINYINT, new String[] { "byte", Byte.class.getName()} );
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.SMALLINT, new String[] { "short", Short.class.getName()} );
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.INTEGER, new String[] { "int", Integer.class.getName()} );
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BIGINT, new String[] { "long", Long.class.getName()} );
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.REAL, new String[] { "float", Float.class.getName()} );
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.FLOAT, new String[] { "double", Double.class.getName()} );
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.DOUBLE, new String[] { "double", Double.class.getName()});
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.DECIMAL, new String[] { "big_decimal", "big_decimal" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.NUMERIC, new String[] { "big_decimal", "big_decimal" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BIT, new String[] { "boolean", Boolean.class.getName()});
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BOOLEAN, new String[] { "boolean", Boolean.class.getName()});
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.CHAR, new String[] { "char", Character.class.getName()});
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.VARCHAR, new String[] { "string", "string" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.LONGVARCHAR, new String[] { "string", "string" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BINARY, new String[] { "binary", "binary" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.VARBINARY, new String[] { "binary", "binary" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.LONGVARBINARY, new String[] { "binary", "binary" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.DATE, new String[] { "date", "date" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.TIME, new String[] { "time", "time" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.TIMESTAMP, new String[] { "timestamp", "timestamp" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.CLOB, new String[] { "clob", "clob" });
		PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.put( Types.BLOB, new String[] { "blob", "blob" });
	}

	public static String getPreferredHibernateType(int sqlType, int size, int precision, int scale, boolean nullable, boolean generatedIdentifier) {
		boolean returnNullable = nullable || generatedIdentifier;
		if ( (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) && scale <= 0) {
			if (precision == 1) {
				return returnNullable?Boolean.class.getName():"boolean";
			}
			else if (precision < 3) {
				return returnNullable?Byte.class.getName():"byte";
			}
			else if (precision < 5) {
				return returnNullable?Short.class.getName():"short";
			}
			else if (precision < 10) {
				return returnNullable?Integer.class.getName():"int";
			}
			else if (precision < 19) {
				return returnNullable?Long.class.getName():"long";
			}
			else {
				return "big_integer";
			}
		}

		if ( sqlType == Types.CHAR && size>1 ) {
			return "string";
		}

		String[] result = PREFERRED_HIBERNATETYPE_FOR_SQLTYPE.get( sqlType );

		if(result==null) {
			return null;
		}
		else if(returnNullable) {
			return result[1];
		}
		else {
			return result[0];
		}
	}

	// ========================================================================
	// Hibernate type name -> Java class
	// ========================================================================

	private static final Map<String, Class<?>> HIBERNATE_TO_JAVA = new HashMap<>();
	private static final Map<String, TemporalType> TEMPORAL_MAP = new HashMap<>();
	private static final Set<String> LOB_TYPES = Set.of("blob", "clob");

	static {
		TEMPORAL_MAP.put("date", TemporalType.DATE);
		TEMPORAL_MAP.put("time", TemporalType.TIME);
		TEMPORAL_MAP.put("timestamp", TemporalType.TIMESTAMP);

		// Primitive types
		HIBERNATE_TO_JAVA.put("int", int.class);
		HIBERNATE_TO_JAVA.put("long", long.class);
		HIBERNATE_TO_JAVA.put("short", short.class);
		HIBERNATE_TO_JAVA.put("byte", byte.class);
		HIBERNATE_TO_JAVA.put("float", float.class);
		HIBERNATE_TO_JAVA.put("double", double.class);
		HIBERNATE_TO_JAVA.put("boolean", boolean.class);
		HIBERNATE_TO_JAVA.put("char", char.class);
		HIBERNATE_TO_JAVA.put("character", char.class);

		// Wrapper types
		HIBERNATE_TO_JAVA.put("java.lang.Integer", Integer.class);
		HIBERNATE_TO_JAVA.put("java.lang.Long", Long.class);
		HIBERNATE_TO_JAVA.put("java.lang.Short", Short.class);
		HIBERNATE_TO_JAVA.put("java.lang.Byte", Byte.class);
		HIBERNATE_TO_JAVA.put("java.lang.Float", Float.class);
		HIBERNATE_TO_JAVA.put("java.lang.Double", Double.class);
		HIBERNATE_TO_JAVA.put("java.lang.Boolean", Boolean.class);
		HIBERNATE_TO_JAVA.put("java.lang.Character", Character.class);

		// String types
		HIBERNATE_TO_JAVA.put("string", String.class);
		HIBERNATE_TO_JAVA.put("java.lang.String", String.class);
		HIBERNATE_TO_JAVA.put("clob", String.class);

		// Numeric types
		HIBERNATE_TO_JAVA.put("big_decimal", BigDecimal.class);
		HIBERNATE_TO_JAVA.put("java.math.BigDecimal", BigDecimal.class);
		HIBERNATE_TO_JAVA.put("big_integer", BigInteger.class);
		HIBERNATE_TO_JAVA.put("java.math.BigInteger", BigInteger.class);

		// Date/time types
		HIBERNATE_TO_JAVA.put("date", Date.class);
		HIBERNATE_TO_JAVA.put("time", Date.class);
		HIBERNATE_TO_JAVA.put("timestamp", Date.class);
		HIBERNATE_TO_JAVA.put("java.util.Date", Date.class);

		// Boolean shorthand types
		HIBERNATE_TO_JAVA.put("yes_no", Boolean.class);
		HIBERNATE_TO_JAVA.put("true_false", Boolean.class);
		HIBERNATE_TO_JAVA.put("numeric_boolean", Boolean.class);

		// Binary types
		HIBERNATE_TO_JAVA.put("binary", byte[].class);
		HIBERNATE_TO_JAVA.put("blob", byte[].class);

		// Serializable fallback
		HIBERNATE_TO_JAVA.put("serializable", Serializable.class);
	}

	public static Class<?> toJavaClass(String hibernateTypeName) {
		if (hibernateTypeName == null) {
			return Object.class;
		}
		Class<?> result = HIBERNATE_TO_JAVA.get(hibernateTypeName);
		if (result != null) {
			return result;
		}
		if (hibernateTypeName.contains(".")) {
			try {
				return Class.forName(hibernateTypeName);
			}
			catch (ClassNotFoundException ignored) {
			}
		}
		return Object.class;
	}

	public static TemporalType toTemporalType(String hibernateTypeName) {
		if (hibernateTypeName == null) {
			return null;
		}
		return TEMPORAL_MAP.get(hibernateTypeName);
	}

	public static boolean isLob(String hibernateTypeName) {
		if (hibernateTypeName == null) {
			return false;
		}
		return LOB_TYPES.contains(hibernateTypeName);
	}

	// ========================================================================
	// Java class -> Hibernate type name
	// ========================================================================

	private static final Map<Class<?>, String> JAVA_TO_HIBERNATE = new HashMap<>();

	static {
		// Primitive types
		JAVA_TO_HIBERNATE.put(int.class, "int");
		JAVA_TO_HIBERNATE.put(long.class, "long");
		JAVA_TO_HIBERNATE.put(short.class, "short");
		JAVA_TO_HIBERNATE.put(byte.class, "byte");
		JAVA_TO_HIBERNATE.put(float.class, "float");
		JAVA_TO_HIBERNATE.put(double.class, "double");
		JAVA_TO_HIBERNATE.put(boolean.class, "boolean");
		JAVA_TO_HIBERNATE.put(char.class, "character");

		// Wrapper types
		JAVA_TO_HIBERNATE.put(Integer.class, "java.lang.Integer");
		JAVA_TO_HIBERNATE.put(Long.class, "java.lang.Long");
		JAVA_TO_HIBERNATE.put(Short.class, "java.lang.Short");
		JAVA_TO_HIBERNATE.put(Byte.class, "java.lang.Byte");
		JAVA_TO_HIBERNATE.put(Float.class, "java.lang.Float");
		JAVA_TO_HIBERNATE.put(Double.class, "java.lang.Double");
		JAVA_TO_HIBERNATE.put(Boolean.class, "java.lang.Boolean");
		JAVA_TO_HIBERNATE.put(Character.class, "java.lang.Character");

		// String
		JAVA_TO_HIBERNATE.put(String.class, "string");

		// Numeric types
		JAVA_TO_HIBERNATE.put(BigDecimal.class, "big_decimal");
		JAVA_TO_HIBERNATE.put(BigInteger.class, "big_integer");

		// Date/time
		JAVA_TO_HIBERNATE.put(Date.class, "timestamp");

		// Binary
		JAVA_TO_HIBERNATE.put(byte[].class, "binary");

		// Serializable fallback
		JAVA_TO_HIBERNATE.put(Serializable.class, "serializable");
	}

	public static String toHibernateType(Class<?> javaClass) {
		if (javaClass == null) {
			return "serializable";
		}
		String result = JAVA_TO_HIBERNATE.get(javaClass);
		return result != null ? result : javaClass.getName();
	}

	public static String toHibernateType(String className) {
		if (className == null) {
			return "serializable";
		}
		try {
			return toHibernateType(Class.forName(className));
		}
		catch (ClassNotFoundException e) {
			return className;
		}
	}

	// ========================================================================
	// Java class name -> JDBC SQL type
	// ========================================================================

	private static final Map<String, Integer> JAVA_TO_JDBC = new HashMap<>();

	static {
		JAVA_TO_JDBC.put(String.class.getName(), Types.VARCHAR);
		JAVA_TO_JDBC.put(Long.class.getName(), Types.BIGINT);
		JAVA_TO_JDBC.put(long.class.getName(), Types.BIGINT);
		JAVA_TO_JDBC.put(Integer.class.getName(), Types.INTEGER);
		JAVA_TO_JDBC.put(int.class.getName(), Types.INTEGER);
		JAVA_TO_JDBC.put(Short.class.getName(), Types.SMALLINT);
		JAVA_TO_JDBC.put(short.class.getName(), Types.SMALLINT);
		JAVA_TO_JDBC.put(Byte.class.getName(), Types.TINYINT);
		JAVA_TO_JDBC.put(byte.class.getName(), Types.TINYINT);
		JAVA_TO_JDBC.put(Float.class.getName(), Types.FLOAT);
		JAVA_TO_JDBC.put(float.class.getName(), Types.FLOAT);
		JAVA_TO_JDBC.put(Double.class.getName(), Types.DOUBLE);
		JAVA_TO_JDBC.put(double.class.getName(), Types.DOUBLE);
		JAVA_TO_JDBC.put(Boolean.class.getName(), Types.BOOLEAN);
		JAVA_TO_JDBC.put(boolean.class.getName(), Types.BOOLEAN);
		JAVA_TO_JDBC.put(BigDecimal.class.getName(), Types.NUMERIC);
		JAVA_TO_JDBC.put(java.sql.Date.class.getName(), Types.DATE);
		JAVA_TO_JDBC.put(java.sql.Time.class.getName(), Types.TIME);
		JAVA_TO_JDBC.put(java.sql.Timestamp.class.getName(), Types.TIMESTAMP);
		JAVA_TO_JDBC.put(Date.class.getName(), Types.TIMESTAMP);
		JAVA_TO_JDBC.put(byte[].class.getName(), Types.VARBINARY);
		JAVA_TO_JDBC.put(Character.class.getName(), Types.CHAR);
		JAVA_TO_JDBC.put(char.class.getName(), Types.CHAR);
	}

	public static int getJdbcTypeCode(String javaClassName) {
		if (javaClassName == null) {
			return Integer.MIN_VALUE;
		}
		Integer jdbcType = JAVA_TO_JDBC.get(javaClassName);
		return jdbcType != null ? jdbcType : Integer.MIN_VALUE;
	}

	// ========================================================================
	// JDBC type name <-> integer constant
	// ========================================================================

	private static Map<String, Integer> jdbcTypes; // Name to value
	private static Map<Integer, String> jdbcTypeValues; // value to Name

	public static String[] getJDBCTypes() {
		checkTypes();
		return jdbcTypes.keySet().toArray( new String[0] );
	}

	public static int getJDBCType(String value) {
		checkTypes();
		Integer number = jdbcTypes.get(value);
		if(number==null) {
			try {
				return Integer.parseInt(value);
			}
			catch (NumberFormatException nfe) {
				throw new MappingException("jdbc-type: " + value + " is not a known JDBC Type nor a valid number");
			}
		}
		else {
			return number;
		}
	}

	public static String getJDBCTypeName(int value) {
		if(jdbcTypeValues==null) {
			jdbcTypeValues = new HashMap<>();
			Field[] fields = Types.class.getFields();
			for ( Field field : fields ) {
				if ( Modifier.isStatic( field.getModifiers() ) ) {
					try {
						jdbcTypeValues.put( (Integer) field.get( Types.class ), field.getName() );
					}
					catch (IllegalArgumentException | IllegalAccessException e) {
						// ignore
					}
				}
			}
		}
		String name = jdbcTypeValues.get( value );
		if(name!=null) {
			return name;
		}
		else {
			return ""+value;
		}
	}

	private static void checkTypes() {
		if(jdbcTypes==null) {
			jdbcTypes = new HashMap<>();
			Field[] fields = Types.class.getFields();
			for ( Field field : fields ) {
				if ( Modifier.isStatic( field.getModifiers() ) ) {
					try {
						jdbcTypes.put( field.getName(), (Integer) field.get( Types.class ) );
					}
					catch (IllegalArgumentException | IllegalAccessException e) {
						// ignore
					}
				}
			}
		}
	}

	// ========================================================================
	// SQL type metadata
	// ========================================================================

	public static boolean typeHasScale(int sqlType) {
		return (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC);
	}

	public static boolean typeHasPrecision(int sqlType) {
		return (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC
				|| sqlType == Types.REAL || sqlType == Types.FLOAT || sqlType == Types.DOUBLE);
	}

	public static boolean typeHasScaleAndPrecision(int sqlType) {
		return typeHasScale(sqlType) && typeHasPrecision(sqlType);
	}

	public static boolean typeHasLength(int sqlType) {
		return (sqlType == Types.CHAR || sqlType == Types.DATE
				|| sqlType == Types.LONGVARCHAR || sqlType == Types.TIME || sqlType == Types.TIMESTAMP
				|| sqlType == Types.VARCHAR );
	}

	// ========================================================================
	// Primitive type checking
	// ========================================================================

	private static final Set<String> PRIMITIVE_TYPES = Set.of(
			"boolean", "byte", "char", "short", "int", "long", "float", "double");

	public static boolean isPrimitiveType(String className) {
		return className != null && PRIMITIVE_TYPES.contains(className);
	}
}
