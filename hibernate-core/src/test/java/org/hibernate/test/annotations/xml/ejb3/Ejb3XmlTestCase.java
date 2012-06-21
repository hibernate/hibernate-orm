/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.xml.ejb3;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import org.hibernate.cfg.annotations.reflection.JPAOverriddenAnnotationReader;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test superclass to provide utility methods for testing the mapping of JPA
 * XML to JPA annotations.  The configuration is built within each test, and no
 * database is used.  Thus, no schema generation or cleanup will be performed.
 */
abstract class Ejb3XmlTestCase extends BaseUnitTestCase {
	protected JPAOverriddenAnnotationReader reader;

	protected void assertAnnotationPresent(Class<? extends Annotation> annotationType) {
		assertTrue(
				"Expected annotation " + annotationType.getSimpleName() + " was not present",
				reader.isAnnotationPresent( annotationType )
		);
	}

	protected void assertAnnotationNotPresent(Class<? extends Annotation> annotationType) {
		assertFalse(
				"Unexpected annotation " + annotationType.getSimpleName() + " was present",
				reader.isAnnotationPresent( annotationType )
		);
	}

	protected JPAOverriddenAnnotationReader getReader(Class<?> entityClass, String fieldName, String ormResourceName)
			throws Exception {
		AnnotatedElement el = getAnnotatedElement( entityClass, fieldName );
		XMLContext xmlContext = getContext( ormResourceName );
		return new JPAOverriddenAnnotationReader( el, xmlContext );
	}

	protected AnnotatedElement getAnnotatedElement(Class<?> entityClass, String fieldName) throws Exception {
		return entityClass.getDeclaredField( fieldName );
	}

	protected XMLContext getContext(String resourceName) throws Exception {
		InputStream is = getClass().getResourceAsStream( resourceName );
		assertNotNull( "Could not load resource " + resourceName, is );
		return getContext( is );
	}

	protected XMLContext getContext(InputStream is) throws Exception {
		XMLContext xmlContext = new XMLContext();
		Document doc = new SAXReader().read( is );
		xmlContext.addDocument( doc );
		return xmlContext;
	}
}
