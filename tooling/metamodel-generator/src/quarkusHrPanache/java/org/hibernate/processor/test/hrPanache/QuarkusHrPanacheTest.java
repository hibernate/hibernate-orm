/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hrPanache;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import io.smallrye.mutiny.Uni;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Gavin King
 */
@CompilationTest
class QuarkusHrPanacheTest {
	@Test
	@WithClasses({ PanacheBook.class })
	void testPanacheEntityMetamodel() throws Exception {
		// Panache entity
		System.out.println( TestUtil.getMetaModelSourceAsString( PanacheBook.class ) );
		Class<?> entityClass = getMetamodelClassFor( PanacheBook.class );
		Assertions.assertNotNull( entityClass );

		// Make sure it has the proper supertype
		Class<?> superclass = entityClass.getSuperclass();
		if ( superclass != null ) {
			Assertions.assertEquals( "io.quarkus.hibernate.reactive.panache.PanacheEntity_", superclass.getName() );
		}

		// Panache static native method generates a static method
		Method method = entityClass.getDeclaredMethod( "hqlBook", Uni.class, String.class );
		Assertions.assertNotNull( method );
		checkUni(method);
		Assertions.assertTrue( Modifier.isStatic( method.getModifiers()) );

		// Panache static native method generates a static method
		method = entityClass.getDeclaredMethod( "findBook", Uni.class, String.class );
		Assertions.assertNotNull( method );
		checkUni(method);
		Assertions.assertTrue( Modifier.isStatic( method.getModifiers() ) );
	}

	@Test
	@WithClasses({ PanacheBook.class, PanacheBookRepository.class })
	void testPanacheRepositoryMetamodel() throws Exception {
		// Panache repository
		System.out.println( TestUtil.getMetaModelSourceAsString( PanacheBookRepository.class ) );
		Class<?> repositoryClass = getMetamodelClassFor( PanacheBookRepository.class );
		Assertions.assertNotNull( repositoryClass );

		// Make sure it has the proper supertype
		Class<?> superclass = repositoryClass.getSuperclass();
		if ( superclass != null ) {
			Assertions.assertEquals( "java.lang.Object", superclass.getName() );
		}

		// Panache native method generates a static method
		Method method = repositoryClass.getDeclaredMethod( "hqlBook", Uni.class, String.class );
		Assertions.assertNotNull( method );
		checkUni(method);
		Assertions.assertTrue( Modifier.isStatic(method.getModifiers()) );

		// Panache native method generates a static method
		method = repositoryClass.getDeclaredMethod( "findBook", Uni.class, String.class );
		Assertions.assertNotNull( method );
		checkUni(method);
		Assertions.assertTrue( Modifier.isStatic( method.getModifiers() ) );
	}

	private void checkUni(Method method) {
		Assertions.assertEquals("io.smallrye.mutiny.Uni<org.hibernate.reactive.mutiny.Mutiny$Session>", method.getGenericParameterTypes()[0].toString());
	}

	@Test
	@WithClasses({ PanacheBook.class, QuarkusBookRepository.class })
	void testQuarkusRepositoryMetamodel() throws Exception {
		// Regular repository
		System.out.println( TestUtil.getMetaModelSourceAsString( QuarkusBookRepository.class ) );
		Class<?> repositoryClass = getMetamodelClassFor( QuarkusBookRepository.class );
		Assertions.assertNotNull( repositoryClass );

		// Make sure it has the proper supertype
		Class<?> superclass = repositoryClass.getSuperclass();
		if ( superclass != null ) {
			Assertions.assertEquals( "java.lang.Object", superclass.getName() );
		}
		Class<?>[] interfaces = repositoryClass.getInterfaces();
		Assertions.assertEquals( 1, interfaces.length );
		Assertions.assertEquals( QuarkusBookRepository.class.getName(), interfaces[0].getName() );

		// Annotated method generates an instance method
		Method method = repositoryClass.getDeclaredMethod( "hqlBook", String.class );
		Assertions.assertNotNull( method );
		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );

		// Annotated method generates an instance method
		method = repositoryClass.getDeclaredMethod( "findBook", String.class );
		Assertions.assertNotNull( method );
		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );

		// Make sure we have only the default constructor
		Constructor<?>[] constructors = repositoryClass.getDeclaredConstructors();
		Assertions.assertNotNull( constructors );
		Assertions.assertEquals( 1, constructors.length );
		Assertions.assertNotNull( repositoryClass.getDeclaredConstructor() );

		// Proper return type
		method = repositoryClass.getDeclaredMethod( "deleteAllBooksVoid" );
		Assertions.assertNotNull( method );
		Assertions.assertEquals("io.smallrye.mutiny.Uni<java.lang.Void>", method.getGenericReturnType().toString());
		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );

		// Proper return type
		method = repositoryClass.getDeclaredMethod( "deleteAllBooksInt" );
		Assertions.assertNotNull( method );
		Assertions.assertEquals("io.smallrye.mutiny.Uni<java.lang.Integer>", method.getGenericReturnType().toString());
		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );
	}

	@Test
	@WithClasses({ PanacheBook.class, BookRepositoryWithSession.class })
	void testBookRepositoryWithSessionMetamodel() throws Exception {
		// Regular repository with default session method
		System.out.println( TestUtil.getMetaModelSourceAsString( BookRepositoryWithSession.class ) );
		Class<?> repositoryClass = getMetamodelClassFor( BookRepositoryWithSession.class );
		Assertions.assertNotNull( repositoryClass );

		// Make sure we have only the default constructor
		Constructor<?>[] constructors = repositoryClass.getDeclaredConstructors();
		Assertions.assertNotNull( constructors );
		Assertions.assertEquals( 1, constructors.length );
		Assertions.assertNotNull( repositoryClass.getDeclaredConstructor() );

		// Make sure we do not override the default session method
		Assertions.assertThrows( NoSuchMethodException.class, () -> repositoryClass.getDeclaredMethod( "mySession" ) );
	}

	// Not supported yet: https://hibernate.atlassian.net/browse/HHH-17960
//	@Test
//	@WithClasses({ PanacheBook.class, JakartaDataBookRepository.class })
//	void testJakartaDataRepositoryMetamodel() throws Exception {
//		// JD repository
//		System.out.println( TestUtil.getMetaModelSourceAsString( JakartaDataBookRepository.class ) );
//		Class<?> repositoryClass = getMetamodelClassFor( JakartaDataBookRepository.class );
//		Assertions.assertNotNull( repositoryClass );
//
//		// Make sure it has the proper supertype
//		Class<?> superclass = repositoryClass.getSuperclass();
//		if ( superclass != null ) {
//			Assertions.assertEquals( "java.lang.Object", superclass.getName() );
//		}
//		Class<?>[] interfaces = repositoryClass.getInterfaces();
//		Assertions.assertEquals( 1, interfaces.length );
//		Assertions.assertEquals( JakartaDataBookRepository.class.getName(), interfaces[0].getName() );
//
//		// Annotated method generates an instance method
//		Method method = repositoryClass.getDeclaredMethod( "hqlBook", String.class );
//		Assertions.assertNotNull( method );
//		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );
//
//		// Annotated method generates an instance method
//		method = repositoryClass.getDeclaredMethod( "findBook", String.class );
//		Assertions.assertNotNull( method );
//		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );
//
//		// Make sure we have the proper constructor
//		Constructor<?> constructor = repositoryClass.getDeclaredConstructor( StatelessSession.class );
//		Assertions.assertNotNull( constructor );
//		Assertions.assertTrue( constructor.isAnnotationPresent( Inject.class ) );
//	}
}
