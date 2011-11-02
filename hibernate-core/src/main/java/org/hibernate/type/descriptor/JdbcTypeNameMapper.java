/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type.descriptor;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class JdbcTypeNameMapper {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, JdbcTypeNameMapper.class.getName());
	private static Map<Integer,String> JDBC_TYPE_MAP = buildJdbcTypeMap();

	private static Map<Integer, String> buildJdbcTypeMap() {
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		Field[] fields = Types.class.getFields();
		if ( fields == null ) {
			throw new HibernateException( "Unexpected problem extracting JDBC type mapping codes from java.sql.Types" );
		}
		for ( Field field : fields ) {
			try {
				final int code = field.getInt( null );
				String old = map.put( code, field.getName() );
                if (old != null) LOG.JavaSqlTypesMappedSameCodeMultipleTimes(code, old, field.getName());
			}
			catch ( IllegalAccessException e ) {
				throw new HibernateException( "Unable to access JDBC type mapping [" + field.getName() + "]", e );
			}
		}
		return Collections.unmodifiableMap( map );
	}

	public static String getTypeName(Integer code) {
		String name = JDBC_TYPE_MAP.get( code );
		if ( name == null ) {
			return "UNKNOWN(" + code + ")";
		}
		return name;
	}
}
