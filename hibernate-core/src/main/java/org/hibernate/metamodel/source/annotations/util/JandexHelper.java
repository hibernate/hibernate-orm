/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Utility methods for working with the jandex annotation index.
 *
 * @author Hardy Ferentschik
 */
public class JandexHelper {
	private JandexHelper() {
	}

	/**
	 * @param classInfo the class info from which to retrieve the annotation instance
	 * @param annotationName the annotation to retrieve from the class info
	 *
	 * @return the single annotation defined on the class or {@code null} in case the annotation is not specified at all
	 *
	 * @throws org.hibernate.AssertionFailure in case there is
	 */
	public static AnnotationInstance getSingleAnnotation(ClassInfo classInfo, DotName annotationName)
			throws AssertionFailure {
		List<AnnotationInstance> annotationList = classInfo.annotations().get( annotationName );
		if ( annotationList == null ) {
			return null;
		}
		else if ( annotationList.size() == 1 ) {
			return annotationList.get( 0 );
		}
		else {
			throw new AssertionFailure(
					"Found more than one instance of the annotation "
							+ annotationList.get( 0 ).name().toString()
							+ ". Expected was one."
			);
		}
	}

	/**
	 * Creates a jandex index for the specified classes
	 *
	 * @param classLoaderService  class loader service
	 * @param classes the classes to index
	 * @return an annotation repository w/ all the annotation discovered in the specified classes
	 */
	public static Index indexForClass(ClassLoaderService classLoaderService, Class<?>... classes) {
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : classes ) {
			InputStream stream = classLoaderService.locateResourceStream(
					clazz.getName().replace( '.', '/' ) + ".class"
			);
			try {
				indexer.index( stream );
			}
			catch ( IOException e ) {
				StringBuilder builder = new StringBuilder();
				builder.append( "[" );
				int count = 0;
				for ( Class<?> c : classes ) {
					builder.append( c.getName() );
					if ( count < classes.length - 1 ) {
						builder.append( "," );
					}
					count++;
				}
				builder.append( "]" );
				throw new HibernateException( "Unable to create annotation index for " + builder.toString());
			}
		}
		return indexer.complete();
	}
}


