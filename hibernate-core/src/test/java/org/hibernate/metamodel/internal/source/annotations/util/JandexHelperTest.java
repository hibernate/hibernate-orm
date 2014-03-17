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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeOverride;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.LockModeType;
import javax.persistence.NamedQuery;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for the helper class {@link org.hibernate.metamodel.source.internal.annotations.util.JandexHelper}.
 *
 * @author Hardy Ferentschik
 */
public class JandexHelperTest extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;
	private ClassLoaderService classLoaderService;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testGetMemberAnnotations() {
		class Foo {
			@Column
			@Basic
			private String bar;
			private String fubar;
		}
		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );

		ClassInfo classInfo = index.getClassByName( DotName.createSimple( Foo.class.getName() ) );
		Map<DotName, List<AnnotationInstance>> memberAnnotations = JandexHelper.getMemberAnnotations(
				classInfo, "bar", serviceRegistry
		);
		assertTrue(
				"property bar should defines @Column annotation",
				memberAnnotations.containsKey( DotName.createSimple( Column.class.getName() ) )
		);
		assertTrue(
				"property bar should defines @Basic annotation",
				memberAnnotations.containsKey( DotName.createSimple( Basic.class.getName() ) )
		);

		memberAnnotations = JandexHelper.getMemberAnnotations( classInfo, "fubar", serviceRegistry );
		assertTrue( "there should be no annotations in fubar", memberAnnotations.isEmpty() );
	}

	@Test
	public void testGettingNestedAnnotation() {
		@AttributeOverride(name = "foo", column = @Column(name = "FOO"))
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.ATTRIBUTE_OVERRIDE );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		// try to retrieve the name
		String name = JandexHelper.getValue( annotationInstance, "name", String.class, classLoaderService );
		assertEquals( "Wrong nested annotation", "foo", name );

		// try to retrieve the nested column annotation instance
		AnnotationInstance columnAnnotationInstance = JandexHelper.getValue(
				annotationInstance,
				"column",
				AnnotationInstance.class,
				classLoaderService
		);
		assertNotNull( columnAnnotationInstance );
		assertEquals(
				"Wrong nested annotation",
				"javax.persistence.Column",
				columnAnnotationInstance.name().toString()
		);
	}

	@Test(expected = AssertionFailure.class)
	public void testTryingToRetrieveWrongType() {
		@AttributeOverride(name = "foo", column = @Column(name = "FOO"))
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.ATTRIBUTE_OVERRIDE );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		JandexHelper.getValue( annotationInstance, "name", Float.class,
				classLoaderService);
	}

	@Test
	public void testRetrieveDefaultEnumElement() {
		@NamedQuery(name = "foo", query = "fubar")
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.NAMED_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		LockModeType lockMode = JandexHelper.getEnumValue( annotationInstance, "lockMode", LockModeType.class,
				classLoaderService );
		assertEquals( "Wrong lock mode", LockModeType.NONE, lockMode );
	}

	@Test
	public void testRetrieveExplicitEnumElement() {
		@NamedQuery(name = "foo", query = "bar", lockMode = LockModeType.OPTIMISTIC)
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.NAMED_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		LockModeType lockMode = JandexHelper.getEnumValue( annotationInstance, "lockMode", LockModeType.class,
				classLoaderService );
		assertEquals( "Wrong lock mode", LockModeType.OPTIMISTIC, lockMode );
	}

	@Test(expected = AssertionFailure.class)
	public void testRetrieveClassParameterAsClass() {
		@NamedNativeQuery(name = "foo", query = "bar", resultClass = Foo.class)
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		JandexHelper.getValue( annotationInstance, "resultClass", Class.class, classLoaderService );
	}

	@Test
	public void testRetrieveClassParameterAsString() {
		@NamedNativeQuery(name = "foo", query = "bar", resultClass = Foo.class)
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		String fqcn = JandexHelper.getValue( annotationInstance, "resultClass", String.class, classLoaderService );
		assertEquals( "Wrong class names", Foo.class.getName(), fqcn );
	}

	@Test
	public void testRetrieveUnknownParameter() {
		@Entity
		class Foo {
		}

		IndexView index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Collection<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.ENTITY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.iterator().next();

		try {
			JandexHelper.getValue( annotationInstance, "foo", String.class, classLoaderService );
			fail();
		}
		catch ( AssertionFailure e ) {
			assertTrue(
					e.getMessage()
							.startsWith( "The annotation javax.persistence.Entity does not define a parameter 'foo'" )
			);
		}
	}

}


