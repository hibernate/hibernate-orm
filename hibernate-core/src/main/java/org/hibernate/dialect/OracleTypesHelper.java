/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;

import org.jboss.logging.Logger;

/**
 * A Helper for dealing with the OracleTypes class
 *
 * @author Steve Ebersole
 */
public class OracleTypesHelper {
	private static final CoreMessageLogger log = Logger.getMessageLogger( CoreMessageLogger.class, OracleTypesHelper.class.getName() );

	/**
	 * Singleton access
	 */
	public static final OracleTypesHelper INSTANCE = new OracleTypesHelper();

	private static final String ORACLE_TYPES_CLASS_NAME = "oracle.jdbc.OracleTypes";
	private static final String DEPRECATED_ORACLE_TYPES_CLASS_NAME = "oracle.jdbc.driver.OracleTypes";

	private final int oracleCursorTypeSqlType;

	private OracleTypesHelper() {
		int typeCode = -99;
		try {
			typeCode = extractOracleCursorTypeValue();
		}
		catch (Exception e) {
			log.warn( "Unable to resolve Oracle CURSOR JDBC type code", e );
		}
		oracleCursorTypeSqlType = typeCode;
	}

	private int extractOracleCursorTypeValue() {
		try {
			return locateOracleTypesClass().getField( "CURSOR" ).getInt( null );
		}
		catch ( Exception se ) {
			throw new HibernateException( "Unable to access OracleTypes.CURSOR value", se );
		}
	}

	private Class locateOracleTypesClass() {
		try {
			return ReflectHelper.classForName( ORACLE_TYPES_CLASS_NAME );
		}
		catch (ClassNotFoundException e) {
			try {
				return ReflectHelper.classForName( DEPRECATED_ORACLE_TYPES_CLASS_NAME );
			}
			catch (ClassNotFoundException e2) {
				throw new HibernateException(
						String.format(
								"Unable to locate OracleTypes class using either known FQN [%s, %s]",
								ORACLE_TYPES_CLASS_NAME,
								DEPRECATED_ORACLE_TYPES_CLASS_NAME
						),
						e
				);
			}
		}
	}

	public int getOracleCursorTypeSqlType() {
		return oracleCursorTypeSqlType;
	}

// initial code as copied from Oracle8iDialect
//
//	private int oracleCursorTypeSqlType = INIT_ORACLETYPES_CURSOR_VALUE;
//
//	public int getOracleCursorTypeSqlType() {
//		if ( oracleCursorTypeSqlType == INIT_ORACLETYPES_CURSOR_VALUE ) {
//			// todo : is there really any reason to keep trying if this fails once?
//			oracleCursorTypeSqlType = extractOracleCursorTypeValue();
//		}
//		return oracleCursorTypeSqlType;
//	}
//
//	private int extractOracleCursorTypeValue() {
//		Class oracleTypesClass;
//		try {
//			oracleTypesClass = ReflectHelper.classForName( ORACLE_TYPES_CLASS_NAME );
//		}
//		catch ( ClassNotFoundException cnfe ) {
//			try {
//				oracleTypesClass = ReflectHelper.classForName( DEPRECATED_ORACLE_TYPES_CLASS_NAME );
//			}
//			catch ( ClassNotFoundException e ) {
//				throw new HibernateException( "Unable to locate OracleTypes class", e );
//			}
//		}
//
//		try {
//			return oracleTypesClass.getField( "CURSOR" ).getInt( null );
//		}
//		catch ( Exception se ) {
//			throw new HibernateException( "Unable to access OracleTypes.CURSOR value", se );
//		}
//	}
}
