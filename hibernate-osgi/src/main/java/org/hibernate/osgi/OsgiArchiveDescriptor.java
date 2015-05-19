/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.persistence.PersistenceException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * ArchiveDescriptor implementation for describing archives in the OSGi sense
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiArchiveDescriptor implements ArchiveDescriptor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( OsgiArchiveDescriptor.class );

	private final Bundle persistenceBundle;
	private final BundleWiring bundleWiring;

	/**
	 * Creates a OsgiArchiveDescriptor
	 *
	 * @param persistenceBundle The bundle being described as an archive
	 */
	@SuppressWarnings("RedundantCast")
	public OsgiArchiveDescriptor(Bundle persistenceBundle) {
		this.persistenceBundle = persistenceBundle;
		bundleWiring = (BundleWiring) persistenceBundle.adapt( BundleWiring.class );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
		final Collection<String> resources = bundleWiring.listResources( "/", "*", BundleWiring.LISTRESOURCES_RECURSE );
		for ( final String resource : resources ) {
			// TODO: Is there a better way to check this?  Karaf is including directories.
			if ( !resource.endsWith( "/" ) ) {
				try {
					// TODO: Is using resource as the names correct?
					final InputStreamAccess inputStreamAccess = new InputStreamAccess() {
						@Override
						public String getStreamName() {
							return resource;
						}
	
						@Override
						public InputStream accessInputStream() {
							return openInputStream();
						}
						
						private InputStream openInputStream() {
							try {
								return persistenceBundle.getResource( resource ).openStream();
							}
							catch ( IOException e ) {
								throw new PersistenceException(
										"Unable to open an InputStream on the OSGi Bundle resource!",
										e );
							}
						}
						
					};
					
					final ArchiveEntry entry = new ArchiveEntry() {
						@Override
						public String getName() {
							return resource;
						}
	
						@Override
						public String getNameWithinArchive() {
							return resource;
						}
	
						@Override
						public InputStreamAccess getStreamAccess() {
							return inputStreamAccess;
						}
					};
					
					context.obtainArchiveEntryHandler( entry ).handleEntry( entry, context );
				}
				catch ( Exception e ) {
					LOG.unableToLoadScannedClassOrResource( e );
				}
			}
		}
	}

}
