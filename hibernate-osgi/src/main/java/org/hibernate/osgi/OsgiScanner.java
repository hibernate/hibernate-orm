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

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;

import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.Scanner;
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * OSGi-specific implementation of the Scanner contract.  Scans the persistence
 * unit Bundle for classes and resources.  The given 'URL jartoScan' is
 * completely ignored.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiScanner implements Scanner {
	
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(
    		CoreMessageLogger.class, OsgiScanner.class.getName() );
	
	private BundleWiring bundleWiring;
	
	private Bundle persistenceBundle;
	
	public OsgiScanner( Bundle persistenceBundle ) {
		this.persistenceBundle = persistenceBundle;
		bundleWiring = (BundleWiring) persistenceBundle.adapt( BundleWiring.class );
	}

	@Override
	public Set<Package> getPackagesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		// As far as I know, BundleWiring#listResources does not support packages.
		return Collections.EMPTY_SET;
	}

	@Override
	public Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		
		Collection<String> classNames = bundleWiring.listResources(
				"/", "*.class", BundleWiring.LISTRESOURCES_RECURSE );
		for (String className : classNames) {
			try {
				URL classUrl = persistenceBundle.getResource( className );
				InputStream is = classUrl.openStream();
				DataInputStream dis = new DataInputStream( is );

				ClassFile cf = new ClassFile(dis);
				AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute( AnnotationsAttribute.visibleTag );
				if ( visible != null ) {
					for ( Class annotation : annotationsToLookFor ) {
						if ( visible.getAnnotation( annotation.getName() ) != null ) {
							classes.add( persistenceBundle.loadClass( cf.getName() ));
							break;
						}
					}
				}

				dis.close();
				is.close();
			}
			catch (Exception e) {
				LOG.unableToLoadScannedClassOrResource( e );
			}
		}
		return classes;
	}

	@Override
	public Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns) {
		Set<NamedInputStream> files = new HashSet<NamedInputStream>();
		for (String filePattern : filePatterns) {
			// BundleWiring#listResources requires that filters take only
			// the filename into consideration, not the path.  "The pattern is
			// only matched against the last element of the resource path."
			// So, strip the path.
			filePattern = filePattern.replaceAll( "(.*/)*", "" );
			Collection<String> resources = bundleWiring.listResources(
					"/", filePattern, BundleWiring.LISTRESOURCES_RECURSE );
			for (String resource : resources) {
				try {
					// TODO: Is using 'resource' as the name correct?  Check what NativeScanner uses.
					files.add( new NamedInputStream( resource, persistenceBundle.getResource( resource ).openStream() ) );
				}
				catch (Exception e) {
					LOG.unableToLoadScannedClassOrResource( e );
				}
			}
		}
		return files;
	}

	@Override
	public Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns) {
		// not used
		return null;
	}

	@Override
	public String getUnqualifiedJarName(URL jarUrl) {
		// not used
		return null;
	}
}
