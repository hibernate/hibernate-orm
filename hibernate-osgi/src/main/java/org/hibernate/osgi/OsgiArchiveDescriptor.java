/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi;

import java.io.InputStream;
import java.util.Collection;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.spi.InputStreamAccess;
import org.hibernate.jpa.boot.spi.NamedInputStream;
import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiArchiveDescriptor implements ArchiveDescriptor {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class,
			OsgiArchiveDescriptor.class.getName() );

	private BundleWiring bundleWiring;

	private Bundle persistenceBundle;

	public OsgiArchiveDescriptor(Bundle persistenceBundle) {
		this.persistenceBundle = persistenceBundle;
		bundleWiring = (BundleWiring) persistenceBundle.adapt( BundleWiring.class );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
		Collection<String> resources = bundleWiring.listResources( "/", "*", BundleWiring.LISTRESOURCES_RECURSE );
		for ( final String resource : resources ) {
			try {
				final InputStream inputStream = persistenceBundle.getResource( resource ).openStream();
				
				// TODO: Is using resource as the names correct?
				
				final InputStreamAccess inputStreamAccess = new InputStreamAccess() {
					@Override
					public String getStreamName() {
						return resource;
					}

					@Override
					public InputStream accessInputStream() {
						return inputStream;
					}

					@Override
					public NamedInputStream asNamedInputStream() {
						return new NamedInputStream( resource, inputStream );
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
