/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.internal.EntityManagerMessageLogger;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public final class LogHelper {
	private static final EntityManagerMessageLogger log = Logger.getMessageLogger( EntityManagerMessageLogger.class, LogHelper.class.getName() );

	private LogHelper() {
	}

	public static void logPersistenceUnitInformation(PersistenceUnitDescriptor descriptor) {
		if ( ! log.isDebugEnabled() ) {
			log.processingPersistenceUnitInfoName( descriptor.getName() );
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append( "PersistenceUnitInfo [\n\t" )
				.append( "name: " )
				.append( descriptor.getName() )
				.append( "\n\t" )
				.append( "persistence provider classname: " )
				.append( descriptor.getProviderClassName() )
				.append( "\n\t" )
				.append( "classloader: " )
				.append( descriptor.getClassLoader() )
				.append( "\n\t" )
				.append( "excludeUnlistedClasses: " )
				.append( descriptor.isExcludeUnlistedClasses() )
				.append( "\n\t" )
				.append( "JTA datasource: " )
				.append( descriptor.getJtaDataSource() )
				.append( "\n\t" )
				.append( "Non JTA datasource: " )
				.append( descriptor.getNonJtaDataSource() )
				.append( "\n\t" )
				.append( "Transaction type: " )
				.append( descriptor.getTransactionType() )
				.append( "\n\t" )
				.append( "PU root URL: " )
				.append( descriptor.getPersistenceUnitRootUrl() )
				.append( "\n\t" )
				.append( "Shared Cache Mode: " )
				.append( descriptor.getSharedCacheMode() )
				.append( "\n\t" )
				.append( "Validation Mode: " )
				.append( descriptor.getValidationMode() )
				.append( "\n\t" );
		sb.append( "Jar files URLs [" );
		List<URL> jarFileUrls = descriptor.getJarFileUrls();
		if ( jarFileUrls != null ) {
			for ( URL url : jarFileUrls ) {
				sb.append( "\n\t\t" ).append( url );
			}
		}
		sb.append( "]\n\t" );
		sb.append( "Managed classes names [" );
		List<String> classNames = descriptor.getManagedClassNames();
		if ( classNames != null ) {
			for ( String className : classNames ) {
				sb.append( "\n\t\t" ).append( className );
			}
		}
		sb.append( "]\n\t" );
		sb.append( "Mapping files names [" );
		List<String> mappingFiles = descriptor.getMappingFileNames();
		if ( mappingFiles != null ) {
			for ( String file : mappingFiles ) {
				sb.append( "\n\t\t" ).append( file );
			}
		}
		sb.append( "]\n\t" );
		sb.append( "Properties [" );
		Properties properties = descriptor.getProperties();
		if (properties != null) {
			Enumeration names = properties.propertyNames();
			while ( names.hasMoreElements() ) {
				String name = (String) names.nextElement();
				sb.append( "\n\t\t" ).append( name ).append( ": " ).append( properties.getProperty( name ) );
			}
		}
		sb.append( "]" );

		log.debug( sb.toString() );
	}
}
