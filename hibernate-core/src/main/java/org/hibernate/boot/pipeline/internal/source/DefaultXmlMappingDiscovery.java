/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;

/// Discovers default XML mappings within admitted archives, independently of type scanning.
///
/// @author Steve Ebersole
final class DefaultXmlMappingDiscovery {
	static void collect(
			MappingSources sources,
			URL root,
			List<URL> jars,
			ResolvedBootstrapSettings settings,
			ContributionDiscoveryContext context) {
		final var roots = new LinkedHashMap<String, URL>();
		if ( root != null ) {
			roots.put( root.toExternalForm(), root );
		}
		if ( jars != null ) {
			jars.forEach( jar -> roots.putIfAbsent( jar.toExternalForm(), jar ) );
		}
		if ( roots.isEmpty() ) {
			return;
		}
		final var factory = HibernatePersistenceConfigurationScanner.determineArchiveDescriptorFactory(
				settings.configurationValues(), context.classLoaderService() );
		for ( var archive : roots.values() ) {
			final var entry = factory.buildArchiveDescriptor( archive ).findEntry( "META-INF/orm.xml" );
			if ( entry != null ) {
				sources.addXmlMappingSource( XmlMappingSource.fromArchiveEntry( entry ) );
			}
		}
	}

	private DefaultXmlMappingDiscovery() {
	}
}
