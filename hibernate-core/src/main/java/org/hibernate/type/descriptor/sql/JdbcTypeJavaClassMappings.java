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
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.mapping.Array;

import org.jboss.logging.Logger;

/**
 * Presents recommended {@literal JDCB typecode <-> Java Class} mappings.  Currently the mappings contained here come
 * from the recommendations defined by the JDBC spec itself, as outlined at
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html#1034737"/>.
 * <p/>
 * Eventually, the plan is to have {@link org.hibernate.dialect.Dialect} and
 * {@link java.sql.DatabaseMetaData#getTypeInfo()} contribute this information.
 *
 * @author Steve Ebersole
 */
public class JdbcTypeJavaClassMappings {
	private static final Logger log = Logger.getLogger( JdbcTypeJavaClassMappings.class );

	public static final JdbcTypeJavaClassMappings INSTANCE = new JdbcTypeJavaClassMappings();

	private final ConcurrentHashMap<Class, Integer> javaClassToJdbcTypeCodeMap;
	private final ConcurrentHashMap<Integer, Class> jdbcTypeCodeToJavaClassMap;

	private JdbcTypeJavaClassMappings() {
		javaClassToJdbcTypeCodeMap = buildJdbcJavaClassMappings();
		jdbcTypeCodeToJavaClassMap = transpose( javaClassToJdbcTypeCodeMap );
	}

	public int determineJdbcTypeCodeForJavaClass(Class cls) {
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

	public Class determineJavaClassForJdbcTypeCode(Integer typeCode) {
		Class cls = jdbcTypeCodeToJavaClassMap.get( typeCode );
		if ( cls != null ) {
			return cls;
		}

		log.debugf(
				"Java Class mapping not known for JDBC type code [%s]; using java.lang.Object",
				typeCode
		);
		return Object.class;
	}

	public Class determineJavaClassForJdbcTypeCode(int typeCode) {
		return determineJavaClassForJdbcTypeCode( Integer.valueOf( typeCode ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static ConcurrentHashMap<Class, Integer> buildJdbcJavaClassMappings() {
		ConcurrentHashMap<Class, Integer> jdbcJavaClassMappings = new ConcurrentHashMap<Class, Integer>();

		// these mappings are the ones outlined specifically in the spec
		jdbcJavaClassMappings.put( String.class, Types.VARCHAR );
		jdbcJavaClassMappings.put( BigDecimal.class, Types.NUMERIC );
		jdbcJavaClassMappings.put( Boolean.class, Types.BIT );
		jdbcJavaClassMappings.put( Integer.class, Types.INTEGER );
		jdbcJavaClassMappings.put( Long.class, Types.BIGINT );
		jdbcJavaClassMappings.put( Float.class, Types.REAL );
		jdbcJavaClassMappings.put( Double.class, Types.DOUBLE );
		jdbcJavaClassMappings.put( byte[].class, Types.LONGVARBINARY );
		jdbcJavaClassMappings.put( java.sql.Date.class, Types.DATE );
		jdbcJavaClassMappings.put( Time.class, Types.TIME );
		jdbcJavaClassMappings.put( Timestamp.class, Types.TIMESTAMP );
		jdbcJavaClassMappings.put( Blob.class, Types.BLOB );
		jdbcJavaClassMappings.put( Clob.class, Types.CLOB );
		jdbcJavaClassMappings.put( Array.class, Types.ARRAY );
		jdbcJavaClassMappings.put( Struct.class, Types.STRUCT );
		jdbcJavaClassMappings.put( Ref.class, Types.REF );
		jdbcJavaClassMappings.put( Class.class, Types.JAVA_OBJECT );

		// additional "common sense" registrations
		jdbcJavaClassMappings.put( Character.class, Types.CHAR );
		jdbcJavaClassMappings.put( char[].class, Types.VARCHAR );
		jdbcJavaClassMappings.put( Character[].class, Types.VARCHAR );
		jdbcJavaClassMappings.put( Byte[].class, Types.LONGVARBINARY );
		jdbcJavaClassMappings.put( java.util.Date.class, Types.TIMESTAMP );
		jdbcJavaClassMappings.put( Calendar.class, Types.TIMESTAMP );

		return jdbcJavaClassMappings;
	}

	private static ConcurrentHashMap<Integer, Class> transpose(ConcurrentHashMap<Class, Integer> javaClassToJdbcTypeCodeMap) {
		final ConcurrentHashMap<Integer, Class> transposed = new ConcurrentHashMap<Integer, Class>();

		for ( Map.Entry<Class,Integer> entry : javaClassToJdbcTypeCodeMap.entrySet() ) {
			transposed.put( entry.getValue(), entry.getKey() );
		}

		return transposed;
	}
}
