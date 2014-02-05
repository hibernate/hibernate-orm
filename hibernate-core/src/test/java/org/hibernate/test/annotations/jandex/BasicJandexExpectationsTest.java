/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.jandex;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hibernate.HibernateException;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicJandexExpectationsTest extends BaseUnitTestCase {
	@Test
	public void test1() {
		Indexer indexer = new Indexer();
		add( Version.class, indexer );
		add( ApplicationServer.class, indexer );

		Index index = indexer.complete();

		index.printSubclasses();
		index.printAnnotations();

		ClassInfo ci = index.getClassByName( DotName.createSimple( ApplicationServer.class.getName() ) );
		List<AnnotationInstance> annotations = ci.annotations().get( DotName.createSimple( javax.persistence.Id.class.getName() ) );
		assertNotNull( annotations );
	}

	@Test
	public void testIndexingSameClassMultipleTimes() {
		Indexer indexer = new Indexer();
		add( Version.class, indexer );
		add( Version.class, indexer );
		add( Version.class, indexer );
		add( ApplicationServer.class, indexer );

		Index index = indexer.complete();

		index.printSubclasses();
		index.printAnnotations();
	}

	private ClassInfo add(Class theClass, Indexer indexer) {
		final String theClassResourceName = '/' + theClass.getName().replace( '.', '/' ) + ".class";

		InputStream stream = getClass().getResourceAsStream( theClassResourceName );

		try {
			return indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for resource " + theClassResourceName, e );
		}
	}
}
