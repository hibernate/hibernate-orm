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
package org.hibernate.engine.jdbc.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines a mapping from JDBC type-code to JDBC type-name as defined by {@link Types}.  Mainly intended for
 * logging/debugging uses.
 *
 * @author Steve Ebersole
 */
public class JdbcTypeCodeNameMap {
	public static final JdbcTypeCodeNameMap INSTANCE = new JdbcTypeCodeNameMap();

	private final ConcurrentHashMap<Integer,String> jdbcTypeCodeMap = generateJdbcTypeCodeMap();

	private static ConcurrentHashMap<Integer,String> generateJdbcTypeCodeMap() {
		final Field[] fields = Types.class.getFields();
		ConcurrentHashMap<Integer,String> cache = new ConcurrentHashMap<Integer,String>( (int)( fields.length * .75 ) + 1 );
		for ( final Field field : fields ) {
			if ( Modifier.isStatic( field.getModifiers() ) ) {
				if ( int.class.equals( field.getType() ) || Integer.class.equals( field.getType() ) ) {
					try {
						cache.put( (Integer) field.get( null ), field.getName() );
					}
					catch (Throwable ignore) {
					}
				}
			}
		}
		return cache;
	}

	public String getJdbcTypeName(int typeCode) {
		return getJdbcTypeName( typeCode, "<unknown:" + typeCode + ">" );
	}

	public String getJdbcTypeName(int typeCode, String defaultReturn) {
		final String known = jdbcTypeCodeMap.get( typeCode );
		return known == null ? defaultReturn : known;
	}
}
