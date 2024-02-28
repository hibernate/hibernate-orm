/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.ormPanache;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import jakarta.persistence.EntityManager;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetamodelClassFor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Gavin King
 */
public class QuarkusOrmPanacheTest extends CompilationTest {
	@Test
	@WithClasses({ PanacheBook.class })
	public void testPanacheEntityMetamodel() throws Exception {
		// Panache entity
		System.out.println( TestUtil.getMetaModelSourceAsString( PanacheBook.class ) );
		Class<?> entityClass = getMetamodelClassFor( PanacheBook.class );
		Assertions.assertNotNull(entityClass);
		
		// Make sure it has the proper supertype
		Class<?> superclass = entityClass.getSuperclass();
		if(superclass != null) {
			Assertions.assertEquals("io.quarkus.hibernate.orm.panache.PanacheEntity_", superclass.getName());
		}
		
		// Panache static native method generates a static method
		Method method = entityClass.getDeclaredMethod("hqlBook", EntityManager.class, String.class);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));

		// Panache static native method generates a static method
		method = entityClass.getDeclaredMethod("findBook", EntityManager.class, String.class);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
	}

	@Test
	@WithClasses({ PanacheBook.class, PanacheBookRepository.class })
	public void testPanacheRepositoryMetamodel() throws Exception {
		// Panache repository
		System.out.println( TestUtil.getMetaModelSourceAsString( PanacheBookRepository.class ) );
		Class<?> repositoryClass = getMetamodelClassFor( PanacheBookRepository.class );
		Assertions.assertNotNull(repositoryClass);
		
		// Make sure it has the proper supertype
		Class<?> superclass = repositoryClass.getSuperclass();
		if(superclass != null) {
			Assertions.assertEquals("java.lang.Object", superclass.getName());
		}
		
		// Panache native method generates a static method
		Method method = repositoryClass.getDeclaredMethod("hqlBook", EntityManager.class, String.class);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));

		// Panache native method generates a static method
		method = repositoryClass.getDeclaredMethod("findBook", EntityManager.class, String.class);
		Assertions.assertNotNull(method);
		Assertions.assertTrue(Modifier.isStatic(method.getModifiers()));
	}
}
