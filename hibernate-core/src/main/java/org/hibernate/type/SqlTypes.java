/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.Internal;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;

import java.sql.Types;

/**
 * Defines a list of constant type codes used to identify generic SQL types.
 * This is an extension of the standard JDBC-defined {@link Types}, defining
 * additional type codes for types that Hibernate supports but which are not
 * recognized by the JDBC specification.
 * <p>
 * Each of these type codes represents an abstraction over a family of
 * similar types in different databases. It's the job of the SQL
 * {@link org.hibernate.dialect.Dialect}, and in particular of the method
 * {@code columnType()}, to interpret these type codes as column type names.
 * <p>
 * A type code is often used as a key to obtain a
 * {@link org.hibernate.type.descriptor.jdbc.JdbcType}, by implementors of
 * {@link org.hibernate.type.descriptor.java.JavaType#getRecommendedJdbcType},
 * or when the {@link org.hibernate.annotations.JdbcTypeCode @JdbcTypeCode}
 * annotation is used, for example.
 * <p>
 * A type code may also be used as a key to obtain a dialect-specific
 * {@link org.hibernate.type.descriptor.sql.DdlType} for the purposes of
 * generating DDL.
 *
 * @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry
 * @see org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry
 *
 * @author Christian Beikov
 */
public class SqlTypes {
	/**
	 * A type code representing generic SQL type {@code BIT}.
	 *
	 * @see Types#BIT
	 */
	public final static int BIT = Types.BIT;

	/**
	 * A type code representing the generic SQL type {@code TINYINT}.
	 *
	 * @see Types#TINYINT
	 * @see org.hibernate.type.descriptor.jdbc.TinyIntJdbcType
	 */
	public final static int TINYINT = Types.TINYINT;

	/**
	 * A type code representing the generic SQL type {@code SMALLINT}.
	 *
	 * @see Types#SMALLINT
	 * @see org.hibernate.type.descriptor.jdbc.SmallIntJdbcType
	 */
	public final static int SMALLINT = Types.SMALLINT;

	/**
	 * A type code representing the generic SQL type {@code INTEGER}.
	 *
	 * @see Types#INTEGER
	 * @see org.hibernate.type.descriptor.jdbc.IntegerJdbcType
	 */
	public final static int INTEGER = Types.INTEGER;

	/**
	 * A type code representing the generic SQL type {@code BIGINT}.
	 *
	 * @see Types#BIGINT
	 * @see org.hibernate.type.descriptor.jdbc.BigIntJdbcType
	 */
	public final static int BIGINT = Types.BIGINT;

	/**
	 * A type code representing the generic SQL type {@code FLOAT}.
	 *
	 * @see Types#FLOAT
	 * @see org.hibernate.type.descriptor.jdbc.FloatJdbcType
	 */
	public final static int FLOAT = Types.FLOAT;

	/**
	 * A type code representing the generic SQL type {@code REAL}.
	 *
	 * @see Types#REAL
	 * @see org.hibernate.type.descriptor.jdbc.RealJdbcType
	 */
	public final static int REAL = Types.REAL;

	/**
	 * A type code representing the generic SQL type {@code DOUBLE}.
	 *
	 * @see Types#DOUBLE
	 * @see org.hibernate.type.descriptor.jdbc.DoubleJdbcType
	 */
	public final static int DOUBLE = Types.DOUBLE;

	/**
	 * A type code representing the generic SQL type {@code NUMERIC}.
	 *
	 * @see Types#NUMERIC
	 * @see org.hibernate.type.descriptor.jdbc.NumericJdbcType
	 */
	public final static int NUMERIC = Types.NUMERIC;

	/**
	 * A type code representing the generic SQL type {@code DECIMAL}.
	 *
	 * @see Types#DECIMAL
	 * @see org.hibernate.type.descriptor.jdbc.DecimalJdbcType
	 */
	public final static int DECIMAL = Types.DECIMAL;

	/**
	 * A type code representing the generic SQL type {@code CHAR}.
	 *
	 * @see Types#CHAR
	 * @see org.hibernate.type.descriptor.jdbc.CharJdbcType
	 */
	public final static int CHAR = Types.CHAR;

	/**
	 * A type code representing the generic SQL type {@code VARCHAR}.
	 *
	 * @see Types#VARCHAR
	 * @see org.hibernate.type.descriptor.jdbc.VarcharJdbcType
	 */
	public final static int VARCHAR = Types.VARCHAR;

	/**
	 * A type code representing the generic SQL type {@code LONGVARCHAR}.
	 * <p>
	 * Interpreted by Hibernate as a {@link #VARCHAR}-like type large enough
	 * to hold a string of maximum length {@link org.hibernate.Length#LONG}.
	 * <p>
	 * Apart from the larger default column length, this type code is treated
	 * as a synonym for {@link #VARCHAR}.
	 *
	 * @see org.hibernate.Length#LONG
	 *
	 * @see Types#LONGVARCHAR
	 * @see org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType
	 */
	public final static int LONGVARCHAR = Types.LONGVARCHAR;

	/**
	 * A type code used internally by the Hibernate
	 * {@link org.hibernate.dialect.Dialect} to identify a
	 * {@link #VARCHAR}-like type large enough to hold any Java string.
	 * <p>
	 * In principle, the type must accommodate strings of length
	 * {@value Integer#MAX_VALUE}, though this is not an absolutely hard
	 * requirement, since such large strings do not occur in practice.
	 *
	 * @see org.hibernate.Length#LONG32
	 */
	public final static int LONG32VARCHAR = 4001;

	/**
	 * A type code representing the generic SQL type {@code DATE}.
	 *
	 * @see Types#DATE
	 * @see org.hibernate.type.descriptor.jdbc.DateJdbcType
	 */
	public final static int DATE = Types.DATE;

	/**
	 * A type code representing the generic SQL type {@code TIME}.
	 *
	 * @see Types#TIME
	 * @see org.hibernate.type.descriptor.jdbc.TimeJdbcType
	 */
	public final static int TIME = Types.TIME;

	/**
	 * A type code representing the generic SQL type {@code TIMESTAMP}.
	 *
	 * @see Types#TIMESTAMP
	 * @see org.hibernate.type.descriptor.jdbc.TimestampJdbcType
	 */
	public final static int TIMESTAMP = Types.TIMESTAMP;

	/**
	 * A type code representing the generic SQL type {@code BINARY}.
	 *
	 * @see Types#BINARY
	 * @see org.hibernate.type.descriptor.jdbc.BinaryJdbcType
	 */
	public final static int BINARY = Types.BINARY;

	/**
	 * A type code representing the generic SQL type {@code VARBINARY}.
	 *
	 * @see Types#VARBINARY
	 * @see org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType
	 */
	public final static int VARBINARY = Types.VARBINARY;

	/**
	 * A type code representing the generic SQL type {@code LONGVARBINARY}.
	 * <p>
	 * Interpreted by Hibernate as a {@link #VARBINARY}-like type large enough
	 * to hold a byte array of maximum length {@link org.hibernate.Length#LONG}.
	 * <p>
	 * Apart from the larger default column length, this type code is treated
	 * as a synonym for {@link #VARBINARY}.
	 *
	 * @see org.hibernate.Length#LONG
	 * @see Types#LONGVARBINARY
	 * @see org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcType
	 */
	public final static int LONGVARBINARY = Types.LONGVARBINARY;

	/**
	 * A type code used by the Hibernate SQL
	 * {@linkplain org.hibernate.dialect.Dialect dialect} to identify a
	 * {@link #VARBINARY}-like type large enough to hold any Java byte array.
	 * <p>
	 * In principle, the type must accommodate arrays of length
	 * {@value Integer#MAX_VALUE}, though this is not an absolutely hard
	 * requirement, since such large arrays do not occur in practice.
	 *
	 * @see org.hibernate.Length#LONG32
	 */
	public final static int LONG32VARBINARY = 4003;

	/**
	 * A type code representing the generic SQL value {@code NULL}.
	 *
	 * @see Types#NULL
	 */
	public final static int NULL = Types.NULL;

	/**
	 * A type code indicating that the SQL type is SQL dialect-specific
	 * and is mapped to a Java object that can be accessed via the methods
	 * {@link java.sql.ResultSet#getObject} and
	 * {@link java.sql.PreparedStatement#setObject}.
	 *
	 * @see Types#OTHER
	 */
	public final static int OTHER = Types.OTHER;

	/**
	 * A type code representing the generic SQL type {@code JAVA_OBJECT}.
	 *
	 * @see Types#JAVA_OBJECT
	 * @see org.hibernate.type.descriptor.jdbc.ObjectJdbcType
	 */
	public final static int JAVA_OBJECT = Types.JAVA_OBJECT;

	/**
	 * A type code representing the generic SQL type {@code DISTINCT}.
	 *
	 * @see Types#DISTINCT
	 */
	public final static int DISTINCT = Types.DISTINCT;

	/**
	 * A type code representing the generic SQL type {@code STRUCT}.
	 *
	 * @see Types#STRUCT
	 */
	public final static int STRUCT = Types.STRUCT;

	/**
	 * A type code representing the generic SQL type {@code ARRAY}.
	 *
	 * @see Types#ARRAY
	 * @see org.hibernate.type.descriptor.jdbc.ArrayJdbcType
	 */
	public final static int ARRAY = Types.ARRAY;

	/**
	 * A type code representing an Oracle-style nested table.
	 *
	 * @see org.hibernate.dialect.type.OracleNestedTableJdbcType
	 */
	public final static int TABLE = 4000;

	/**
	 * A type code representing the generic SQL type {@code BLOB}.
	 *
	 * @see Types#BLOB
	 * @see org.hibernate.type.descriptor.jdbc.BlobJdbcType
	 */
	public final static int BLOB = Types.BLOB;

	/**
	 * A type code representing the generic SQL type {@code CLOB}.
	 *
	 * @see Types#CLOB
	 * @see org.hibernate.type.descriptor.jdbc.ClobJdbcType
	 */
	public final static int CLOB = Types.CLOB;

	/**
	 * A type code representing the generic SQL type {@code REF}.
	 *
	 * @see Types#REF
	 */
	public final static int REF = Types.REF;

	/**
	 * A type code representing the generic SQL type {@code DATALINK}.
	 *
	 * @see Types#DATALINK
	 */
	public final static int DATALINK = Types.DATALINK;

	/**
	 * A type code representing the generic SQL type {@code BOOLEAN}.
	 *
	 * @see Types#BOOLEAN
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	 * @see org.hibernate.type.descriptor.jdbc.BooleanJdbcType
	 */
	public final static int BOOLEAN = Types.BOOLEAN;

	/**
	 * A type code representing the generic SQL type {@code ROWID}.
	 *
	 * @see Types#ROWID
	 */
	public final static int ROWID = Types.ROWID;

	/**
	 * A type code representing the generic SQL type {@code NCHAR}.
	 *
	 * @see Types#NCHAR
	 * @see org.hibernate.type.descriptor.jdbc.NCharJdbcType
	 */
	public static final int NCHAR = Types.NCHAR;

	/**
	 * A type code representing the generic SQL type {@code NVARCHAR}.
	 *
	 * @see Types#NVARCHAR
	 * @see org.hibernate.type.descriptor.jdbc.NVarcharJdbcType
	 */
	public static final int NVARCHAR = Types.NVARCHAR;

	/**
	 * A type code representing the generic SQL type {@code LONGNVARCHAR}.
	 * <p>
	 * Interpreted by Hibernate as an {@link #NVARCHAR}-like type large enough
	 * to hold a string of maximum length {@link org.hibernate.Length#LONG}.
	 * <p>
	 * Apart from the larger default column length, this type code is treated
	 * as a synonym for {@link #NVARCHAR}.
	 *
	 * @see org.hibernate.Length#LONG
	 * @see Types#LONGNVARCHAR
	 * @see org.hibernate.type.descriptor.jdbc.LongNVarcharJdbcType
	 */
	public static final int LONGNVARCHAR = Types.LONGNVARCHAR;

	/**
	 * A type code used internally by the Hibernate
	 * {@link org.hibernate.dialect.Dialect} to identify an
	 * {@link #NVARCHAR}-like type large enough to hold any Java string.
	 * <p>
	 * In principle, the type must accommodate strings of length
	 * {@value Integer#MAX_VALUE}, though this is not an absolutely hard
	 * requirement, since such large strings do not occur in practice.
	 *
	 * @see org.hibernate.Length#LONG32
	 */
	public final static int LONG32NVARCHAR = 4002;

	/**
	 * A type code representing the generic SQL type {@code NCLOB}.
	 *
	 * @see Types#NCLOB
	 * @see org.hibernate.type.descriptor.jdbc.NClobJdbcType
	 */
	public static final int NCLOB = Types.NCLOB;

	/**
	 * A type code representing the generic SQL type {@code XML}.
	 *
	 * @see Types#SQLXML
	 * @see org.hibernate.type.descriptor.jdbc.XmlJdbcType
	 */
	public static final int SQLXML = Types.SQLXML;

	/**
	 * A type code representing the generic SQL type {@code REF CURSOR}.
	 *
	 * @see Types#REF_CURSOR
	 */
	public static final int REF_CURSOR = Types.REF_CURSOR;

	/**
	 * A type code representing identifies the generic SQL type
	 * {@code TIME WITH TIMEZONE}.
	 *
	 * @see Types#TIME_WITH_TIMEZONE
	 */
	public static final int TIME_WITH_TIMEZONE = Types.TIME_WITH_TIMEZONE;

	/**
	 * A type code representing the generic SQL type
	 * {@code TIMESTAMP WITH TIMEZONE}.
	 *
	 * @see Types#TIMESTAMP_WITH_TIMEZONE
	 * @see org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType
	 */
	public static final int TIMESTAMP_WITH_TIMEZONE = Types.TIMESTAMP_WITH_TIMEZONE;

	// Misc types

	/**
	 * A type code representing the generic SQL type {@code UUID}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_UUID_JDBC_TYPE
	 * @see org.hibernate.type.descriptor.jdbc.UUIDJdbcType
	 */
	public static final int UUID = 3000;

	/**
	 * A type code representing the generic SQL type {@code JSON}.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.JsonJdbcType
	 */
	public static final int JSON = 3001;

	/**
	 * A type code representing the generic SQL type {@code INET} for IPv4
	 * or IPv6 addresses.
	 *
	 * @see org.hibernate.dialect.type.PostgreSQLInetJdbcType
	 */
	public static final int INET = 3002;

	/**
	 * A type code representing the generic SQL type {@code TIMESTAMP},
	 * where the value is given in UTC, instead of in the system or
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JDBC_TIME_ZONE
	 * JDBC} timezone.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_INSTANT_JDBC_TYPE
	 * @see org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType
	 * @see org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType
	 * @see org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType
	 */
	public static final int TIMESTAMP_UTC = 3003;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type
	 * {@code MATERIALIZED_BLOB}.
	 *
	 * This type is used when JDBC access should use {@link #VARBINARY} semantics,
	 * but the {@link org.hibernate.type.descriptor.sql.DdlType} should be based on {@link #BLOB}.
	 */
	@Internal
	public static final int MATERIALIZED_BLOB = 3004;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type
	 * {@code MATERIALIZED_CLOB}.
	 *
	 * This type is used when JDBC access should use {@link #VARCHAR} semantics,
	 * but the {@link org.hibernate.type.descriptor.sql.DdlType} should be based on {@link #CLOB}.
	 */
	@Internal
	public static final int MATERIALIZED_CLOB = 3005;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type
	 * {@code MATERIALIZED_NCLOB}.
	 *
	 * This type is used when JDBC access should use {@link #NVARCHAR} semantics,
	 * but the {@link org.hibernate.type.descriptor.sql.DdlType} should be based on {@link #NCLOB}.
	 */
	@Internal
	public static final int MATERIALIZED_NCLOB = 3006;

	/**
	 * A type code representing the generic SQL type {@code TIME},
	 * where the value is given in UTC, instead of in the system or
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JDBC_TIME_ZONE
	 * JDBC} timezone.
	 *
	 * @see org.hibernate.annotations.TimeZoneStorageType#NORMALIZE_UTC
	 * @see org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType
	 * @see org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType
	 */
	public static final int TIME_UTC = 3007;


	// Java Time (java.time) "virtual" JdbcTypes

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.Instant}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.InstantJdbcType
	 */
	public static final int INSTANT = 3008;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.LocalDateTime}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType
	 */
	public static final int LOCAL_DATE_TIME = 3009;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.LocalDate}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.LocalDateJdbcType
	 */
	public static final int LOCAL_DATE = 3010;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.LocalTime}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType
	 */
	public static final int LOCAL_TIME = 3011;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.OffsetDateTime}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType
	 */
	public static final int OFFSET_DATE_TIME = 3012;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.OffsetTime}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType
	 */
	public static final int OFFSET_TIME = 3013;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.ZonedDateTime}
	 * as a JDBC type using {@linkplain java.sql.ResultSet#getObject} and
	 * {@linkplain java.sql.PreparedStatement#setObject} which JDBC requires compliant
	 * drivers to support.
	 *
	 * @see org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType
	 */
	public static final int ZONED_DATE_TIME = 3014;

	/**
	 * A type code representing a "virtual mapping" of {@linkplain java.time.Duration}.
	 *
	 * @see Types#NUMERIC
	 * @see org.hibernate.type.descriptor.jdbc.DurationJdbcType
	 */
	public static final int DURATION = 3015;

	/**
	 * A type code for an array of struct objects.
	 */
	public static final int STRUCT_ARRAY = 3016;

	/**
	 * A type code representing an Oracle-style nested table for a struct.
	 *
	 * @see org.hibernate.dialect.type.OracleNestedTableJdbcType
	 */
	public final static int STRUCT_TABLE = 3017;

	/**
	 * A type code for an array of json objects.
	 */
	public static final int JSON_ARRAY = 3018;

	/**
	 * A type code for an array of xml objects.
	 */
	public static final int XML_ARRAY = 3019;

	// Interval types

	/**
	 * A type code representing the generic SQL type {@code INTERVAL SECOND}
	 * for a temporal duration given terms of seconds and fractional seconds.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_DURATION_JDBC_TYPE
	 * @see org.hibernate.dialect.type.PostgreSQLIntervalSecondJdbcType
	 * @see org.hibernate.dialect.type.H2DurationIntervalSecondJdbcType
	 */
	public static final int INTERVAL_SECOND = 3100;

	// Geometry types

	/**
	 * A type code representing the generic SQL type {@code GEOMETRY}.
	 */
	public static final int GEOMETRY = 3200;

	/**
	 * A type code representing the generic SQL type {@code POINT}.
	 */
	public static final int POINT = 3201;

	/**
	 * A type code representing the generic SQL type {@code GEOGRAPHY}.
	 *
	 * @since 6.0.1
	 */
	public static final int GEOGRAPHY = 3250;

	/**
	 * A type code representing a SQL {@code ENUM} type for databases like
	 * {@link org.hibernate.dialect.MySQLDialect MySQL} where {@code ENUM}
	 * types do not have names.
	 *
	 * @see EnumJdbcType
	 *
	 * @since 6.3
	 */
	public static final int ENUM = 6000;

	/**
	 * A type code representing a SQL {@code ENUM} type for databases like
	 * {@link org.hibernate.dialect.PostgreSQLDialect PostgreSQL} or
	 * {@link org.hibernate.dialect.OracleDialect Oracle} where
	 * {@code ENUM} types must have names.
	 * <p>
	 * A named enum type is declared in DDL using {@code create type ... as enum}
	 * or {@code create type ... as domain}.
	 *
	 * @see org.hibernate.dialect.type.PostgreSQLEnumJdbcType
	 * @see org.hibernate.dialect.type.OracleEnumJdbcType
	 *
	 * @since 6.3
	 */
	public static final int NAMED_ENUM = 6001;

	/**
	 * A type code representing a SQL {@code ENUM} type for databases like
	 * {@link org.hibernate.dialect.MySQLDialect MySQL} where {@code ENUM}
	 * types do not have names. Enum values are ordered by ordinal.
	 *
	 * @see OrdinalEnumJdbcType
	 *
	 * @since 6.5
	 */
	public static final int ORDINAL_ENUM = 6002;

	/**
	 * A type code representing a SQL {@code ENUM} type for databases like
	 * {@link org.hibernate.dialect.PostgreSQLDialect PostgreSQL} where
	 * {@code ENUM} types must have names. Enum values are ordered by ordinal.
	 *
	 * @see org.hibernate.dialect.type.PostgreSQLEnumJdbcType
	 *
	 * @since 6.5
	 */
	public static final int NAMED_ORDINAL_ENUM = 6003;


	/**
	 * A type code representing an {@code embedding vector} type for databases
	 * like {@link org.hibernate.dialect.PostgreSQLDialect PostgreSQL},
	 * {@link org.hibernate.dialect.OracleDialect Oracle 23ai} and {@link org.hibernate.dialect.MariaDBDialect MariaDB}.
	 * An embedding vector essentially is a {@code float[]} with a fixed size.
	 *
	 * @since 6.4
	 */
	public static final int VECTOR = 10_000;

	/**
	 * A type code representing a single-byte integer vector type for Oracle 23ai database.
	 */
	public static final int VECTOR_INT8 = 10_001;

	/**
	 * A type code representing a single-precision floating-point vector type for Oracle 23ai database.
	 */
	public static final int VECTOR_FLOAT32 = 10_002;

	/**
	 * A type code representing a double-precision floating-point type for Oracle 23ai database.
	 */
	public static final int VECTOR_FLOAT64 = 10_003;

	private SqlTypes() {
	}

	/**
	 * Does the given JDBC type code represent some sort of
	 * numeric type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isNumericType(int typeCode) {
		switch (typeCode) {
			case Types.BIT:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.DOUBLE:
			case Types.REAL:
			case Types.FLOAT:
			case Types.NUMERIC:
			case Types.DECIMAL:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Is this a type with a length, that is, is it
	 * some kind of character string or binary string?
	 *
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isStringType(int typeCode) {
		switch (typeCode) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.NCHAR:
			case Types.NVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given JDBC type code represent some sort of
	 * character string type?
	 *
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isCharacterOrClobType(int typeCode) {
		switch (typeCode) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.NCHAR:
			case Types.NVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.CLOB:
			case Types.NCLOB:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given JDBC type code represent some sort of
	 * character string type?
	 *
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isCharacterType(int typeCode) {
		switch (typeCode) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.NCHAR:
			case Types.NVARCHAR:
			case Types.LONGNVARCHAR:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given JDBC type code represent some sort of
	 * variable-length character string type?
	 *
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isVarcharType(int typeCode) {
		switch (typeCode) {
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.NVARCHAR:
			case Types.LONGNVARCHAR:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given JDBC type code represent some sort of
	 * variable-length binary string type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isVarbinaryType(int typeCode) {
		switch (typeCode) {
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given JDBC type code represent some sort of
	 * variable-length binary string or BLOB type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isBinaryType(int typeCode) {
		switch ( typeCode ) {
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.BLOB:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given typecode represent one of the two SQL decimal types?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isNumericOrDecimal(int typeCode) {
		switch ( typeCode ) {
			case NUMERIC:
			case DECIMAL:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given typecode represent a SQL floating point type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isFloatOrRealOrDouble(int typeCode) {
		switch ( typeCode ) {
			case FLOAT:
			case REAL:
			case DOUBLE:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given typecode represent a SQL integer type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isIntegral(int typeCode) {
		switch ( typeCode ) {
			case INTEGER:
			case BIGINT:
			case SMALLINT:
			case TINYINT:
				return true;
			default:
				return false;
		}
	}

	@Internal
	public static boolean isSmallOrTinyInt(int typeCode) {
		switch ( typeCode ) {
			case SMALLINT:
			case TINYINT:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given typecode represent a SQL date, time, or timestamp type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean isTemporalType(int typeCode) {
		switch ( typeCode ) {
			case DATE:
			case TIME:
			case TIME_WITH_TIMEZONE:
			case TIME_UTC:
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
			case INSTANT:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given typecode represent a SQL {@code interval} type?
	 */
	public static boolean isIntervalType(int typeCode) {
		return typeCode == INTERVAL_SECOND;
	}

	/**
	 * Does the given typecode represent a {@code duration} type?
	 */
	public static boolean isDurationType(int typeCode) {
		return typeCode == DURATION;
	}

	/**
	 * Does the given typecode represent a SQL date or timestamp type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean hasDatePart(int typeCode) {
		switch ( typeCode ) {
			case DATE:
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the given typecode represent a SQL time or timestamp type?
	 * @param typeCode a JDBC type code from {@link Types}
	 */
	public static boolean hasTimePart(int typeCode) {
		switch ( typeCode ) {
			case TIME:
			case TIME_WITH_TIMEZONE:
			case TIME_UTC:
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the typecode represent a spatial (Geometry or Geography) type.
	 *
	 * @param typeCode - a JDBC type code
	 */
	public static boolean isSpatialType(int typeCode) {
		switch ( typeCode ) {
			case GEOMETRY:
			case POINT:
			case GEOGRAPHY:
				return true;
			default:
				return false;
		}
	}

	public static boolean isEnumType(int typeCode) {
		switch ( typeCode ) {
			case ENUM:
			case NAMED_ENUM:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the typecode represent a JSON type.
	 *
	 * @param typeCode - a JDBC type code
	 * @since 7.0
	 */
	public static boolean isJsonType(int typeCode) {
		switch ( typeCode ) {
			case JSON:
			case JSON_ARRAY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the typecode represent a JSON type or a type that can be implicitly cast to JSON.
	 *
	 * @param typeCode - a JDBC type code
	 * @since 7.0
	 */
	public static boolean isImplicitJsonType(int typeCode) {
		switch ( typeCode ) {
			case JSON:
			case JSON_ARRAY:
				return true;
			default:
				return isCharacterOrClobType( typeCode );
		}
	}

	/**
	 * Does the typecode represent a XML type.
	 *
	 * @param typeCode - a JDBC type code
	 * @since 7.0
	 */
	public static boolean isXmlType(int typeCode) {
		switch ( typeCode ) {
			case SQLXML:
			case XML_ARRAY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Does the typecode represent an XML type or a type that can be implicitly cast to XML.
	 *
	 * @param typeCode - a JDBC type code
	 * @since 7.0
	 */
	public static boolean isImplicitXmlType(int typeCode) {
		switch ( typeCode ) {
			case SQLXML:
			case XML_ARRAY:
				return true;
			default:
				return isCharacterOrClobType( typeCode );
		}
	}
}
