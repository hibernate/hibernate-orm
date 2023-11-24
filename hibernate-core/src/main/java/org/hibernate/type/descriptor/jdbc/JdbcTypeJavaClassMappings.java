/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.mapping.Array;
import org.hibernate.type.SqlTypes;

import org.jboss.logging.Logger;

/**
 * Maintains the JDBC recommended mappings for JDBC type-code to/from Java Class
 * as defined in <em>Appendix B: Data Type Conversion Tables</em> of the JDBC
 * Specification.
 *
 * @author Steve Ebersole
 */
//TODO: Eventually, the plan is to have {@link org.hibernate.dialect.Dialect} and
//      {@link java.sql.DatabaseMetaData#getTypeInfo()} contribute this information.
public class JdbcTypeJavaClassMappings {
	private static final Logger log = Logger.getLogger( JdbcTypeJavaClassMappings.class );

	public static final JdbcTypeJavaClassMappings INSTANCE = new JdbcTypeJavaClassMappings();

	private final ConcurrentHashMap<Class<?>, Integer> javaClassToJdbcTypeCodeMap;
	private final ConcurrentHashMap<Integer, Class<?>> jdbcTypeCodeToJavaClassMap;

	private JdbcTypeJavaClassMappings() {
		javaClassToJdbcTypeCodeMap = buildJavaClassToJdbcTypeCodeMappings();
		jdbcTypeCodeToJavaClassMap = buildJdbcTypeCodeToJavaClassMappings();
	}

	/**
	 * For the given Java type, determine the JDBC recommended JDBC type.
	 * <p>
	 * This includes the mappings defined in <em>TABLE B-2: Java Types Mapped to JDBC Types</em>
	 * and <em>TABLE B-4: Java Object Types Mapped to JDBC Types</em>, as well as some additional
	 * "common sense" mappings.
	 */
	public int determineJdbcTypeCodeForJavaClass(Class<?> cls) {
		Integer typeCode = javaClassToJdbcTypeCodeMap.get( cls );
		if ( typeCode != null ) {
			return typeCode;
		}

		int specialCode = cls.hashCode();
		log.debug(
				"JDBC type code mapping not known for class [" + cls.getName() + "]; using custom code [" + specialCode + "]"
		);
		return specialCode;
	}

	/**
	 * For the given JDBC type, determine the JDBC recommended Java type.
	 * <p>
	 * These mappings are defined by <em>TABLE B-1: JDBC Types Mapped to Java Types</em>.
	 */
	public Class<?> determineJavaClassForJdbcTypeCode(Integer typeCode) {
		Class<?> cls = jdbcTypeCodeToJavaClassMap.get( typeCode );
		if ( cls != null ) {
			return cls;
		}

		log.debugf(
				"Java Class mapping not known for JDBC type code [%s]; using java.lang.Object",
				typeCode
		);
		return Object.class;
	}

	/**
	 * @see #determineJavaClassForJdbcTypeCode(Integer)
	 */
	public Class<?> determineJavaClassForJdbcTypeCode(int typeCode) {
		return determineJavaClassForJdbcTypeCode( Integer.valueOf( typeCode ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static ConcurrentHashMap<Class<?>, Integer> buildJavaClassToJdbcTypeCodeMappings() {
		final ConcurrentHashMap<Class<?>, Integer> workMap = new ConcurrentHashMap<>();

		// these mappings are the ones outlined specifically in the spec
		workMap.put( String.class, SqlTypes.VARCHAR );
		workMap.put( BigDecimal.class, SqlTypes.NUMERIC );
		workMap.put( BigInteger.class, SqlTypes.NUMERIC );
		workMap.put( Boolean.class, SqlTypes.BIT );
		workMap.put( Byte.class, SqlTypes.TINYINT );
		workMap.put( Short.class, SqlTypes.SMALLINT );
		workMap.put( Integer.class, SqlTypes.INTEGER );
		workMap.put( Long.class, SqlTypes.BIGINT );
		workMap.put( Float.class, SqlTypes.REAL );
		workMap.put( Double.class, SqlTypes.DOUBLE );
		workMap.put( byte[].class, SqlTypes.VARBINARY );
		workMap.put( java.sql.Date.class, SqlTypes.DATE );
		workMap.put( Time.class, SqlTypes.TIME );
		workMap.put( Timestamp.class, SqlTypes.TIMESTAMP );
		workMap.put( LocalTime.class, SqlTypes.TIME );
		workMap.put( OffsetTime.class, SqlTypes.TIME_WITH_TIMEZONE );
		workMap.put( LocalDate.class, SqlTypes.DATE );
		workMap.put( LocalDateTime.class, SqlTypes.TIMESTAMP );
		workMap.put( OffsetDateTime.class, SqlTypes.TIMESTAMP_WITH_TIMEZONE );
		workMap.put( ZonedDateTime.class, SqlTypes.TIMESTAMP_WITH_TIMEZONE );
		workMap.put( Instant.class, SqlTypes.TIMESTAMP_UTC );
		workMap.put( Blob.class, SqlTypes.BLOB );
		workMap.put( Clob.class, SqlTypes.CLOB );
		workMap.put( Array.class, SqlTypes.ARRAY );
		workMap.put( Struct.class, SqlTypes.STRUCT );
		workMap.put( Ref.class, SqlTypes.REF );
		workMap.put( Class.class, SqlTypes.JAVA_OBJECT );
		workMap.put( RowId.class, SqlTypes.ROWID );
		workMap.put( SQLXML.class, SqlTypes.SQLXML );
		workMap.put( UUID.class, SqlTypes.UUID );
		workMap.put( InetAddress.class, SqlTypes.INET );
		workMap.put( Inet4Address.class, SqlTypes.INET );
		workMap.put( Inet6Address.class, SqlTypes.INET );
		workMap.put( Duration.class, SqlTypes.INTERVAL_SECOND );


		// additional "common sense" registrations
		workMap.put( Character.class, SqlTypes.CHAR );
		workMap.put( char[].class, SqlTypes.VARCHAR );
//		workMap.put( Character[].class, SqlTypes.VARCHAR );
//		workMap.put( Byte[].class, SqlTypes.VARBINARY );
		workMap.put( java.util.Date.class, SqlTypes.TIMESTAMP );
		workMap.put( Calendar.class, SqlTypes.TIMESTAMP );

		return workMap;
	}

	private static ConcurrentHashMap<Integer, Class<?>> buildJdbcTypeCodeToJavaClassMappings() {
		final ConcurrentHashMap<Integer, Class<?>> workMap = new ConcurrentHashMap<>();

		workMap.put( SqlTypes.CHAR, String.class );
		workMap.put( SqlTypes.VARCHAR, String.class );
		workMap.put( SqlTypes.LONGVARCHAR, String.class );
		workMap.put( SqlTypes.NCHAR, String.class );
		workMap.put( SqlTypes.NVARCHAR, String.class );
		workMap.put( SqlTypes.LONGNVARCHAR, String.class );
		workMap.put( SqlTypes.NUMERIC, BigDecimal.class );
		workMap.put( SqlTypes.DECIMAL, BigDecimal.class );
		workMap.put( SqlTypes.BIT, Boolean.class );
		workMap.put( SqlTypes.BOOLEAN, Boolean.class );
		workMap.put( SqlTypes.TINYINT, Byte.class );
		workMap.put( SqlTypes.SMALLINT, Short.class );
		workMap.put( SqlTypes.INTEGER, Integer.class );
		workMap.put( SqlTypes.BIGINT, Long.class );
		workMap.put( SqlTypes.REAL, Float.class );
		workMap.put( SqlTypes.DOUBLE, Double.class );
		workMap.put( SqlTypes.FLOAT, Double.class );
		workMap.put( SqlTypes.BINARY, byte[].class );
		workMap.put( SqlTypes.VARBINARY, byte[].class );
		workMap.put( SqlTypes.LONGVARBINARY, byte[].class );
		workMap.put( SqlTypes.DATE, java.sql.Date.class );
		workMap.put( SqlTypes.TIME, Time.class );
		workMap.put( SqlTypes.TIMESTAMP, Timestamp.class );
		workMap.put( SqlTypes.TIME_WITH_TIMEZONE, OffsetTime.class );
		workMap.put( SqlTypes.TIMESTAMP_WITH_TIMEZONE, OffsetDateTime.class );
		workMap.put( SqlTypes.BLOB, Blob.class );
		workMap.put( SqlTypes.CLOB, Clob.class );
		workMap.put( SqlTypes.NCLOB, NClob.class );
		workMap.put( SqlTypes.ARRAY, Array.class );
		workMap.put( SqlTypes.STRUCT, Struct.class );
		workMap.put( SqlTypes.REF, Ref.class );
		workMap.put( SqlTypes.JAVA_OBJECT, Object.class );
		workMap.put( SqlTypes.ROWID, RowId.class );
		workMap.put( SqlTypes.SQLXML, SQLXML.class );
		workMap.put( SqlTypes.UUID, UUID.class );
		workMap.put( SqlTypes.JSON, String.class );
		workMap.put( SqlTypes.INET, InetAddress.class );
		workMap.put( SqlTypes.TIMESTAMP_UTC, Instant.class );
		workMap.put( SqlTypes.INTERVAL_SECOND, Duration.class );

		return workMap;
	}
}
