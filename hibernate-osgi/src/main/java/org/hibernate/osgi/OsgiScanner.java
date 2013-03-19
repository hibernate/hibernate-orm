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
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

/**
 * @author Brett Meyer
 */
public class OsgiScanner implements Scanner {
	
	private BundleWiring bundleWiring;
	
	private OsgiClassLoader classLoader;
	
	public OsgiScanner( BundleContext context, OsgiClassLoader classLoader ) {
		// TODO: The (BundleWiring) cast shouldn't be necessary.  My jdk7
		// compiler was complaining that adapt returns an Object, even though
		// it doesn't...
		bundleWiring = (BundleWiring) context.getBundle().adapt( BundleWiring.class );
		this.classLoader = classLoader;
	}

	@Override
	public Set<Package> getPackagesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		// TODO Auto-generated method stub
		return Collections.EMPTY_SET;
	}

	@Override
	public Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		
		Collection<String> classNames = bundleWiring.listResources(
				"/", "*.class", BundleWiring.LISTRESOURCES_RECURSE );
		for (String className : classNames) {
			try {
				URL classUrl = classLoader.getResource( className );
				InputStream is = classUrl.openStream();
				DataInputStream dis = new DataInputStream( is );

				ClassFile cf = new ClassFile(dis);
				AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute( AnnotationsAttribute.visibleTag );
				if ( visible != null ) {
					for ( Class annotation : annotationsToLookFor ) {
						if ( visible.getAnnotation( annotation.getName() ) != null ) {
							classes.add( classLoader.loadClass( cf.getName() ));
							System.out.println("GETCLASSESINJAR: " + className);
							break;
						}
					}
				}

				dis.close();
				is.close();
			}
			catch (Exception e) {
				// TODO
				e.printStackTrace();
			}
		}
		return classes;
	}

	@Override
	public Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns) {
		Set<NamedInputStream> files = new HashSet<NamedInputStream>();
		for (String filePattern : filePatterns) {
			filePattern = filePattern.replaceAll( "\\*\\*\\/", "" );
			Collection<String> resources = bundleWiring.listResources(
					"/", filePattern, BundleWiring.LISTRESOURCES_RECURSE );
			for (String resource : resources) {
				try {
					// TODO: Is using 'resource' as the name correct?  Check what NativeScanner uses.
					files.add( new NamedInputStream( resource, classLoader.getResource( resource ).openStream() ) );
					System.out.println("GETFILESINJAR: " + resource);
				}
				catch (Exception e) {
					// TODO
					e.printStackTrace();
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
