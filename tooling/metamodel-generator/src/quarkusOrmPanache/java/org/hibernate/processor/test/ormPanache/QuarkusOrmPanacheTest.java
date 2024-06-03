/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.ormPanache;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Note: JD test is in jakartaData tests, due to requiring Java 17
public class QuarkusOrmPanacheTest extends CompilationTest {
	@Test
	@WithClasses({ PanacheBook.class })
	public void testPanacheEntityMetamodel() throws Exception {
		// Panache entity
		System.out.println( TestUtil.getMetaModelSourceAsString( PanacheBook.class ) );
		Class<?> entityClass = getMetamodelClassFor( PanacheBook.class );
		Assertions.assertNotNull( entityClass );
		
		// Make sure it has the proper supertype
		Class<?> superclass = entityClass.getSuperclass();
		if ( superclass != null ) {
			Assertions.assertEquals( "io.quarkus.hibernate.orm.panache.PanacheEntity_", superclass.getName() );
		}
		
		// Panache static native method generates a static method
		Method method = entityClass.getDeclaredMethod( "hqlBook", EntityManager.class, String.class );
		Assertions.assertNotNull( method );
		Assertions.assertTrue( Modifier.isStatic( method.getModifiers()) );

		// Panache static native method generates a static method
		method = entityClass.getDeclaredMethod( "findBook", EntityManager.class, String.class );
		Assertions.assertNotNull( method );
		Assertions.assertTrue( Modifier.isStatic( method.getModifiers() ) );
	}

	@Test
	@WithClasses({ PanacheBook.class, PanacheBookRepository.class })
	public void testPanacheRepositoryMetamodel() throws Exception {
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
		Method method = repositoryClass.getDeclaredMethod( "hqlBook", EntityManager.class, String.class );
		Assertions.assertNotNull( method );
		Assertions.assertTrue( Modifier.isStatic(method.getModifiers()) );

		// Panache native method generates a static method
		method = repositoryClass.getDeclaredMethod( "findBook", EntityManager.class, String.class );
		Assertions.assertNotNull( method );
		Assertions.assertTrue( Modifier.isStatic( method.getModifiers() ) );
	}

	@Test
	@WithClasses({ PanacheBook.class, QuarkusBookRepository.class })
	public void testQuarkusRepositoryMetamodel() throws Exception {
		// Panache repository
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
		
		// Make sure we have the proper constructor
		Constructor<?> constructor = repositoryClass.getDeclaredConstructor( EntityManager.class );
		Assertions.assertNotNull( constructor );
		Assertions.assertTrue( constructor.isAnnotationPresent( Inject.class ) );
	}
}
