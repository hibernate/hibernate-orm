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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.AttributeOverride;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.LockModeType;
import javax.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for the helper class {@link JandexHelper}.
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
		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );

		ClassInfo classInfo = index.getClassByName( DotName.createSimple( Foo.class.getName() ) );
		Map<DotName, List<AnnotationInstance>> memberAnnotations = JandexHelper.getMemberAnnotations(
				classInfo, "bar"
		);
		assertTrue(
				"property bar should defines @Column annotation",
				memberAnnotations.containsKey( DotName.createSimple( Column.class.getName() ) )
		);
		assertTrue(
				"property bar should defines @Basic annotation",
				memberAnnotations.containsKey( DotName.createSimple( Basic.class.getName() ) )
		);

		memberAnnotations = JandexHelper.getMemberAnnotations( classInfo, "fubar" );
		assertTrue( "there should be no annotations in fubar", memberAnnotations.isEmpty() );
	}

	@Test
	public void testGettingNestedAnnotation() {
		@AttributeOverride(name = "foo", column = @Column(name = "FOO"))
		class Foo {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.ATTRIBUTE_OVERRIDE );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		// try to retrieve the name
		String name = JandexHelper.getValue( annotationInstance, "name", String.class );
		assertEquals( "Wrong nested annotation", "foo", name );

		// try to retrieve the nested column annotation instance
		AnnotationInstance columnAnnotationInstance = JandexHelper.getValue(
				annotationInstance,
				"column",
				AnnotationInstance.class
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

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.ATTRIBUTE_OVERRIDE );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		JandexHelper.getValue( annotationInstance, "name", Float.class );
	}

	@Test
	public void testRetrieveDefaultEnumElement() {
		@NamedQuery(name = "foo", query = "fubar")
		class Foo {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.NAMED_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		LockModeType lockMode = JandexHelper.getEnumValue( annotationInstance, "lockMode", LockModeType.class );
		assertEquals( "Wrong lock mode", LockModeType.NONE, lockMode );
	}

	@Test
	public void testRetrieveExplicitEnumElement() {
		@NamedQuery(name = "foo", query = "bar", lockMode = LockModeType.OPTIMISTIC)
		class Foo {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.NAMED_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		LockModeType lockMode = JandexHelper.getEnumValue( annotationInstance, "lockMode", LockModeType.class );
		assertEquals( "Wrong lock mode", LockModeType.OPTIMISTIC, lockMode );
	}

	@Test
	public void testRetrieveStringArray() {
		class Foo {
			@org.hibernate.annotations.Index(name = "index", columnNames = { "a", "b", "c" })
			private String foo;
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( HibernateDotNames.INDEX );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		String[] columnNames = JandexHelper.getValue( annotationInstance, "columnNames", String[].class );
		Assert.assertTrue( columnNames.length == 3 );
	}

	@Test(expected = AssertionFailure.class)
	public void testRetrieveClassParameterAsClass() {
		@NamedNativeQuery(name = "foo", query = "bar", resultClass = Foo.class)
		class Foo {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		JandexHelper.getValue( annotationInstance, "resultClass", Class.class );
	}

	@Test
	public void testRetrieveClassParameterAsString() {
		@NamedNativeQuery(name = "foo", query = "bar", resultClass = Foo.class)
		class Foo {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		String fqcn = JandexHelper.getValue( annotationInstance, "resultClass", String.class );
		assertEquals( "Wrong class names", Foo.class.getName(), fqcn );
	}

	@Test
	public void testRetrieveUnknownParameter() {
		@Entity
		class Foo {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.ENTITY );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		try {
			JandexHelper.getValue( annotationInstance, "foo", String.class );
			fail();
		}
		catch ( AssertionFailure e ) {
			assertTrue(
					e.getMessage()
							.startsWith( "The annotation javax.persistence.Entity does not define a parameter 'foo'" )
			);
		}
	}


	@Test
	public void testPrimitiveAnnotationAttributeTypes() {
		@Converter( autoApply = true )
		class MyConverter implements AttributeConverter<URL,String> {

			@Override
			public String convertToDatabaseColumn(URL attribute) {
				return attribute.toExternalForm();
			}

			@Override
			public URL convertToEntityAttribute(String dbData) {
				try {
					return new URL( dbData );
				}
				catch (MalformedURLException e) {
					throw new HibernateException( "Could not convert string [" + dbData + "] to url", e );
				}
			}
		}

		Index index = JandexHelper.indexForClass( classLoaderService, MyConverter.class );
		List<AnnotationInstance> annotationInstances = index.getAnnotations( JPADotNames.CONVERTER );
		assertTrue( annotationInstances.size() == 1 );
		AnnotationInstance annotationInstance = annotationInstances.get( 0 );

		boolean value = JandexHelper.getValue( annotationInstance, "autoApply", boolean.class );
		Assert.assertTrue( value );
	}
}


