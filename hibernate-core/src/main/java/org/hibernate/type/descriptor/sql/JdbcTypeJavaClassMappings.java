/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.sql;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.internal.JdbcTypeCodeNameMap;

/**
 * Presents recommended {@literal JDCB typecode <-> Java Class} mappings.  Currently the recommendations
 * contained here come from the JDBC spec itself (as outlined at
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html#1034737"/>), plus a few
 * "common sense" extras.
 * <p/>
 * Eventually, the plan is to have {@link org.hibernate.dialect.Dialect} contribute this information.
 *
 * @author Steve Ebersole
 */
public class JdbcTypeJavaClassMappings {
	private static final Logger log = Logger.getLogger( JdbcTypeJavaClassMappings.class );

	public static final JdbcTypeJavaClassMappings INSTANCE = new JdbcTypeJavaClassMappings();

	private final ConcurrentHashMap<Class, Integer> javaClassToJdbcTypeCodeMap = new ConcurrentHashMap<Class, Integer>();
	private final ConcurrentHashMap<Integer, Class> jdbcTypeCodeToJavaClassMap = new ConcurrentHashMap<Integer, Class>();

	public JdbcTypeJavaClassMappings() {
		// first build the baseline
		addToBoth( Types.VARCHAR, String.class );
		addToBoth( Types.BIT, Boolean.class );
		addToBoth( Types.INTEGER, Integer.class );
		addToBoth( Types.BIGINT, Long.class );
		addToBoth( Types.REAL, Float.class );
		addToBoth( Types.DOUBLE, Double.class );
		addToBoth( Types.NUMERIC, BigDecimal.class );
		addToBoth( Types.LONGVARBINARY, byte[].class );
		addToBoth( Types.DATE, Date.class );
		addToBoth( Types.TIME, Time.class );
		addToBoth( Types.TIMESTAMP, Timestamp.class );
		addToBoth( Types.BLOB, Blob.class );
		addToBoth( Types.CLOB, Clob.class );
		addToBoth( Types.NCLOB, NClob.class );
		addToBoth( Types.ARRAY, Array.class );
		addToBoth( Types.STRUCT, Struct.class );
		addToBoth( Types.REF, Ref.class );
		addToBoth( Types.JAVA_OBJECT, Class.class );

		// now add additional typeCode->javaType mappings
		jdbcTypeCodeToJavaClassMap.put( Types.NVARCHAR, String.class );
		jdbcTypeCodeToJavaClassMap.put( Types.LONGVARCHAR, String.class );
		jdbcTypeCodeToJavaClassMap.put( Types.LONGNVARCHAR, String.class );
		jdbcTypeCodeToJavaClassMap.put( Types.CHAR, String.class );
		jdbcTypeCodeToJavaClassMap.put( Types.NCHAR, String.class );
		jdbcTypeCodeToJavaClassMap.put( Types.VARBINARY, byte[].class );
		jdbcTypeCodeToJavaClassMap.put( Types.OTHER, Object.class );

		// then add additional javaType->typeCode mappings
		javaClassToJdbcTypeCodeMap.put( Character.class, Types.CHAR );
		javaClassToJdbcTypeCodeMap.put( char[].class, Types.VARCHAR );
		javaClassToJdbcTypeCodeMap.put( Character[].class, Types.VARCHAR );
		javaClassToJdbcTypeCodeMap.put( Byte[].class, Types.LONGVARBINARY );
		javaClassToJdbcTypeCodeMap.put( Date.class, Types.TIMESTAMP );
		javaClassToJdbcTypeCodeMap.put( Calendar.class, Types.TIMESTAMP );
	}

	private void addToBoth(int typeCode, Class javaType ) {
		jdbcTypeCodeToJavaClassMap.put( typeCode, javaType );
		javaClassToJdbcTypeCodeMap.put( javaType, typeCode );
	}

	@SuppressWarnings("UnnecessaryUnboxing")
	public int determineJdbcTypeCodeForJavaClass(Class cls) {
		Integer typeCode = javaClassToJdbcTypeCodeMap.get( cls );
		if ( typeCode != null ) {
			return typeCode.intValue();
		}

		int specialCode = cls.hashCode();
		log.debug(
				"JDBC type code mapping not known for class [" + cls.getName() + "]; using custom code [" + specialCode + "]"
		);
		return specialCode;
	}

	@SuppressWarnings("UnnecessaryUnboxing")
	public Class determineJavaClassForJdbcTypeCode(int typeCode) {
		Class cls = jdbcTypeCodeToJavaClassMap.get( Integer.valueOf( typeCode ) );
		if ( cls != null ) {
			return cls;
		}

		log.debugf(
				"Java Class mapping not known for JDBC type code [%s]; using java.lang.Object",
				typeCode
		);
		return Object.class;
	}

	public static void main(String... args) {
		final JdbcTypeJavaClassMappings me = new JdbcTypeJavaClassMappings();

		System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		System.out.println( "jdbcTypeCode -> javaType mappings ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		for ( Map.Entry<Integer, Class> entry : me.jdbcTypeCodeToJavaClassMap.entrySet() ) {
			final Integer typeCode = entry.getKey();
			System.out.println(
					String.format(
							"%s (%s) -> %s",
							typeCode,
							JdbcTypeCodeNameMap.INSTANCE.getJdbcTypeName( typeCode ),
							getName( entry.getValue() )
					)
			);
		}
		System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

		System.out.println();

		System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		System.out.println( "jdbcTypeCode -> javaType mappings ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		for ( Map.Entry<Class, Integer> entry : me.javaClassToJdbcTypeCodeMap.entrySet() ) {
			final Integer typeCode = entry.getValue();
			System.out.println(
					String.format(
							"%s -> %s (%s)",
							getName( entry.getKey() ),
							typeCode,
							JdbcTypeCodeNameMap.INSTANCE.getJdbcTypeName( typeCode )
					)
			);
		}
		System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
	}

	private static String getName(Class javaType) {
		if ( ! javaType.isArray() ) {
			return javaType.getName();
		}

		return javaType.getSimpleName();
	}
}