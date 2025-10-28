/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;


/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public final class LogHelper {

	private LogHelper() {
	}

	public static void logPersistenceUnitInformation(PersistenceUnitDescriptor descriptor) {
		if ( JPA_LOGGER.isDebugEnabled() ) {
			final var builder = new StringBuilder();
			builder.append( "PersistenceUnitInfo [\n\t" )
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
					.append( descriptor.getPersistenceUnitTransactionType() )
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

			builder.append( "Jar files URLs [" );
			List<URL> jarFileUrls = descriptor.getJarFileUrls();
			if ( jarFileUrls != null ) {
				for ( URL url : jarFileUrls ) {
					builder.append( "\n\t\t" ).append( url );
				}
			}
			builder.append( "]\n\t" );

			builder.append( "Managed classes names [" );
			List<String> classNames = descriptor.getManagedClassNames();
			if ( classNames != null ) {
				for ( String className : classNames ) {
					builder.append( "\n\t\t" ).append( className );
				}
			}
			builder.append( "]\n\t" );

			builder.append( "Mapping files names [" );
			List<String> mappingFiles = descriptor.getMappingFileNames();
			if ( mappingFiles != null ) {
				for ( String file : mappingFiles ) {
					builder.append( "\n\t\t" ).append( file );
				}
			}
			builder.append( "]\n\t" );

			builder.append( "Properties [" );
			Properties properties = descriptor.getProperties();
			if ( properties != null ) {
				Enumeration<?> names = properties.propertyNames();
				while ( names.hasMoreElements() ) {
					String name = (String) names.nextElement();
					builder.append( "\n\t\t" ).append( name ).append( ": " ).append( properties.getProperty( name ) );
				}
			}
			builder.append( "]" );

			JPA_LOGGER.processingPersistenceUnitInfoDetails( builder.toString() );
		}
		else {
			JPA_LOGGER.processingPersistenceUnitInfo( descriptor.getName() );
		}
	}
}
