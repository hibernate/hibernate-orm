/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public final class LogHelper {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( LogHelper.class );

	private LogHelper() {
	}

	public static void logPersistenceUnitInformation(PersistenceUnitDescriptor descriptor) {
		if ( ! LOG.isDebugEnabled() ) {
			LOG.processingPersistenceUnitInfoName( descriptor.getName() );
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
			Enumeration<?> names = properties.propertyNames();
			while ( names.hasMoreElements() ) {
				String name = (String) names.nextElement();
				sb.append( "\n\t\t" ).append( name ).append( ": " ).append( properties.getProperty( name ) );
			}
		}
		sb.append( "]" );

		LOG.debug( sb.toString() );
	}
}
