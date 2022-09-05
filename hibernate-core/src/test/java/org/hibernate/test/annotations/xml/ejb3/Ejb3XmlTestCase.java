/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.cfg.annotations.reflection.internal.JPAXMLOverriddenAnnotationReader;
import org.hibernate.cfg.annotations.reflection.internal.XMLContext;
import org.hibernate.internal.util.xml.XMLMappingHelper;

import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test superclass to provide utility methods for testing the mapping of JPA
 * XML to JPA annotations.  The configuration is built within each test, and no
 * database is used.  Thus, no schema generation or cleanup will be performed.
 */
public abstract class Ejb3XmlTestCase extends BaseUnitTestCase {

	protected JPAXMLOverriddenAnnotationReader reader;

	private BootstrapContextImpl bootstrapContext;

	protected Ejb3XmlTestCase() {
	}

	@Before
	public void init() {
		bootstrapContext = new BootstrapContextImpl();
	}

	@After
	public void destroy() {
		bootstrapContext.close();
	}

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

	protected JPAXMLOverriddenAnnotationReader getReader(Class<?> entityClass, String fieldName, String ormResourceName)
			throws Exception {
		AnnotatedElement el = getAnnotatedElement( entityClass, fieldName );
		XMLContext xmlContext = getContext( ormResourceName );
		return new JPAXMLOverriddenAnnotationReader( el, xmlContext, bootstrapContext );
	}

	protected AnnotatedElement getAnnotatedElement(Class<?> entityClass, String fieldName) throws Exception {
		return entityClass.getDeclaredField( fieldName );
	}

	protected XMLContext getContext(String resourceName) throws Exception {
		InputStream is = getClass().getResourceAsStream( resourceName );
		assertNotNull( "Could not load resource " + resourceName, is );
		return getContext( is, resourceName );
	}

	protected XMLContext getContext(InputStream is, String resourceName) throws Exception {
		XMLMappingHelper xmlHelper = new XMLMappingHelper();
		JaxbEntityMappings mappings = xmlHelper.readOrmXmlMappings( is, resourceName );
		XMLContext context = new XMLContext( bootstrapContext );
		context.addDocument( mappings );
		return context;
	}
}
