/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * @author Emmanuel Bernard
 */
public final class LogHelper {
	private LogHelper() {
	}

	public static String logPersistenceUnitInfo(PersistenceUnitInfo unitInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append( "PersistenceUnitInfo [\n\t" )
				.append( "name: " )
				.append( unitInfo.getPersistenceUnitName() )
				.append( "\n\t" )
				.append( "persistence provider classname: " )
				.append( unitInfo.getPersistenceProviderClassName() )
				.append( "\n\t" )
				.append( "classloader: " )
				.append( unitInfo.getClassLoader() )
				.append( "\n\t" )
				.append( "Temporary classloader: " )
				.append( unitInfo.getNewTempClassLoader() )
				.append( "\n\t" )
				.append( "excludeUnlistedClasses: " )
				.append( unitInfo.excludeUnlistedClasses() )
				.append( "\n\t" )
				.append( "JTA datasource: " )
				.append( unitInfo.getJtaDataSource() )
				.append( "\n\t" )
				.append( "Non JTA datasource: " )
				.append( unitInfo.getNonJtaDataSource() )
				.append( "\n\t" )
				.append( "Transaction type: " )
				.append( unitInfo.getTransactionType() )
				.append( "\n\t" )
				.append( "PU root URL: " )
				.append( unitInfo.getPersistenceUnitRootUrl() )
				.append( "\n\t" )
				.append( "Shared Cache Mode: " )
				.append( unitInfo.getSharedCacheMode() )
				.append( "\n\t" )
				.append( "Validation Mode: " )
				.append( unitInfo.getValidationMode() )
				.append( "\n\t" );
		sb.append( "Jar files URLs [" );
		List<URL> jarFileUrls = unitInfo.getJarFileUrls();
		for ( URL url : jarFileUrls ) {
			sb.append( "\n\t\t" ).append( url );
		}
		sb.append( "]\n\t" );
		sb.append( "Managed classes names [" );
		List<String> classesNames = unitInfo.getManagedClassNames();
		for ( String clazz : classesNames ) {
			sb.append( "\n\t\t" ).append( clazz );
		}
		sb.append( "]\n\t" );
		sb.append( "Mapping files names [" );
		List<String> mappingFiles = unitInfo.getMappingFileNames();
		for ( String file : mappingFiles ) {
			sb.append( "\n\t\t" ).append( file );
		}
		sb.append( "]\n\t" );
		sb.append( "Properties [" );
		Properties properties = unitInfo.getProperties();
		if (properties != null) {
			Enumeration names = properties.propertyNames();
			while ( names.hasMoreElements() ) {
				String name = (String) names.nextElement();
				sb.append( "\n\t\t" ).append( name ).append( ": " ).append( properties.getProperty( name ) );
			}
		}
		sb.append( "]" );
		return sb.toString();
	}

}
