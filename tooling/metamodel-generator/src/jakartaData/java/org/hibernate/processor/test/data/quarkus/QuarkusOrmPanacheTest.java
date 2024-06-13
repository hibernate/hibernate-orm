/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.quarkus;

import org.hibernate.StatelessSession;
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

/**
 * @author Gavin King
 */
public class QuarkusOrmPanacheTest extends CompilationTest {

	@Test
	@WithClasses({ PanacheBook.class, JakartaDataBookRepository.class })
	public void testJakartaDataRepositoryMetamodel() throws Exception {
		// JD repository
		System.out.println( TestUtil.getMetaModelSourceAsString( JakartaDataBookRepository.class ) );
		Class<?> repositoryClass = getMetamodelClassFor( JakartaDataBookRepository.class );
		Assertions.assertNotNull( repositoryClass );
		
		// Make sure it has the proper supertype
		Class<?> superclass = repositoryClass.getSuperclass();
		if ( superclass != null ) {
			Assertions.assertEquals( "java.lang.Object", superclass.getName() );
		}
		Class<?>[] interfaces = repositoryClass.getInterfaces();
		Assertions.assertEquals( 1, interfaces.length );
		Assertions.assertEquals( JakartaDataBookRepository.class.getName(), interfaces[0].getName() );
		
		// Annotated method generates an instance method
		Method method = repositoryClass.getDeclaredMethod( "hqlBook", String.class );
		Assertions.assertNotNull( method );
		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );

		// Annotated method generates an instance method
		method = repositoryClass.getDeclaredMethod( "findBook", String.class );
		Assertions.assertNotNull( method );
		Assertions.assertFalse( Modifier.isStatic( method.getModifiers() ) );
		
		// Make sure we have the proper constructor
		Constructor<?> constructor = repositoryClass.getDeclaredConstructor( StatelessSession.class );
		Assertions.assertNotNull( constructor );
		Assertions.assertTrue( constructor.isAnnotationPresent( Inject.class ) );
	}
}
