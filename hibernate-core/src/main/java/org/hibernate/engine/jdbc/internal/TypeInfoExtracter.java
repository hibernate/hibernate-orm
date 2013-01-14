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
package org.hibernate.engine.jdbc.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Helper to extract type information from {@link DatabaseMetaData JDBC metadata}
 *
 * @author Steve Ebersole
 */
public class TypeInfoExtracter {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TypeInfoExtracter.class.getName());

	private TypeInfoExtracter() {
	}

	/**
	 * Perform the extraction
	 *
	 * @param metaData The JDBC metadata
	 *
	 * @return The extracted metadata
	 */
	public static LinkedHashSet<TypeInfo> extractTypeInfo(DatabaseMetaData metaData) {
		LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();
		try {
			ResultSet resultSet = metaData.getTypeInfo();
			try {
				while ( resultSet.next() ) {
					typeInfoSet.add(
							new TypeInfo(
									resultSet.getString( "TYPE_NAME" ),
									resultSet.getInt( "DATA_TYPE" ),
									interpretCreateParams( resultSet.getString( "CREATE_PARAMS" ) ),
									resultSet.getBoolean( "UNSIGNED_ATTRIBUTE" ),
									resultSet.getInt( "PRECISION" ),
									resultSet.getShort( "MINIMUM_SCALE" ),
									resultSet.getShort( "MAXIMUM_SCALE" ),
									resultSet.getBoolean( "FIXED_PREC_SCALE" ),
									resultSet.getString( "LITERAL_PREFIX" ),
									resultSet.getString( "LITERAL_SUFFIX" ),
									resultSet.getBoolean( "CASE_SENSITIVE" ),
									TypeSearchability.interpret( resultSet.getShort( "SEARCHABLE" ) ),
									TypeNullability.interpret( resultSet.getShort( "NULLABLE" ) )
							)
					);
				}
			}
			catch ( SQLException e ) {
				LOG.unableToAccessTypeInfoResultSet( e.toString() );
			}
			finally {
				try {
					resultSet.close();
				}
				catch ( SQLException e ) {
					LOG.unableToReleaseTypeInfoResultSet();
				}
			}
		}
		catch ( SQLException e ) {
			LOG.unableToRetrieveTypeInfoResultSet( e.toString() );
		}

		return typeInfoSet;
	}

	private static String[] interpretCreateParams(String value) {
		if ( value == null || value.length() == 0 ) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
		return value.split( "," );
	}
}
