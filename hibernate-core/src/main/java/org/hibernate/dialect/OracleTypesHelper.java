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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.jboss.logging.Logger;

/**
 * A Helper for dealing with the OracleTypes class
 *
 * @author Steve Ebersole
 */
public class OracleTypesHelper {
	
	// On demand helper, but cannot be singleton in order to properly support ClassLoaderService.
	
	private static final CoreMessageLogger log = Logger.getMessageLogger( CoreMessageLogger.class, OracleTypesHelper.class.getName() );

	private static final String ORACLE_TYPES_CLASS_NAME = "oracle.jdbc.OracleTypes";
	private static final String DEPRECATED_ORACLE_TYPES_CLASS_NAME = "oracle.jdbc.driver.OracleTypes";

	private int oracleCursorTypeSqlType = -99;
	
	private ClassLoaderService classLoaderService;
	
	public OracleTypesHelper(ServiceRegistry serviceRegistry) {
		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	public int getOracleCursorTypeSqlType() {
		if ( oracleCursorTypeSqlType == -99 ) {
			try {
				oracleCursorTypeSqlType = extractOracleCursorTypeValue();
			}
			catch (Exception e) {
				log.warn( "Unable to resolve Oracle CURSOR JDBC type code", e );
			}
		}
		return oracleCursorTypeSqlType;
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
			return classLoaderService.classForName( ORACLE_TYPES_CLASS_NAME );
		}
		catch (ClassLoadingException e) {
			try {
				return classLoaderService.classForName( DEPRECATED_ORACLE_TYPES_CLASS_NAME );
			}
			catch (ClassLoadingException e2) {
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
}
