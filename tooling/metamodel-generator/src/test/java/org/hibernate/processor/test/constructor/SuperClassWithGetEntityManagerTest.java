/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.processor.test.constructor;

import static org.hibernate.processor.test.util.TestUtil.assertAbsenceOfNonDefaultConstructorInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

/**
 * Test various scenarios where a superclass of an entity has a {@code getEntityManager()} method.
 * <p>
 * The superclass may be itself an entity, or a mapped superclass, or not mapped at all.
 * <p>
 * The method may be static or not.
 */
@TestForIssue(jiraKey = "HHH-17683")
public class SuperClassWithGetEntityManagerTest extends CompilationTest {
	@Test
	@WithClasses({ EntityWithInstanceGetEntityManager.class, EntityExtendingEntityWithInstanceGetEntityManager.class })
	public void entityWithInstanceGetEntityManager() {
		doTest( EntityWithInstanceGetEntityManager.class, EntityExtendingEntityWithInstanceGetEntityManager.class );
	}

	@Test
	@WithClasses({ EntityWithStaticGetEntityManager.class, EntityExtendingEntityWithStaticGetEntityManager.class })
	public void entityWithStaticGetEntityManager() {
		doTest( EntityWithStaticGetEntityManager.class, EntityExtendingEntityWithStaticGetEntityManager.class );
	}

	@Test
	@WithClasses({
			NonEntityWithInstanceGetEntityManager.class, EntityExtendingNonEntityWithInstanceGetEntityManager.class
	})
	public void nonEntityWithInstanceGetEntityManager() {
		doTest(
				NonEntityWithInstanceGetEntityManager.class,
				EntityExtendingNonEntityWithInstanceGetEntityManager.class
		);
	}

	@Test
	@WithClasses({
			NonEntityWithStaticGetEntityManager.class,
			EntityExtendingNonEntityWithStaticGetEntityManager.class
	})
	public void nonEntityWithStaticGetEntityManager() {
		doTest(
				NonEntityWithStaticGetEntityManager.class,
				EntityExtendingNonEntityWithStaticGetEntityManager.class
		);
	}

	@Test
	@WithClasses({
			MapperSuperClassWithInstanceGetEntityManager.class,
			EntityExtendingMapperSuperClassWithInstanceGetEntityManager.class
	})
	public void mappedSuperClassWithInstanceGetEntityManager() {
		doTest(
				MapperSuperClassWithInstanceGetEntityManager.class,
				EntityExtendingMapperSuperClassWithInstanceGetEntityManager.class
		);
	}

	@Test
	@WithClasses({
			MapperSuperClassWithStaticGetEntityManager.class,
			EntityExtendingMapperSuperClassWithStaticGetEntityManager.class
	})
	public void mappedSuperClassWithStaticGetEntityManager() {
		doTest(
				MapperSuperClassWithStaticGetEntityManager.class,
				EntityExtendingMapperSuperClassWithStaticGetEntityManager.class
		);
	}

	@Test
	@WithClasses({
			NonEntityWithInstanceGetEntityManager.class,
			MapperSuperClassExtendingNonEntityWithInstanceGetEntityManager.class,
			EntityExtendingMapperSuperClassExtendingNonEntityWithInstanceGetEntityManager.class
	})
	public void mappedSuperClassExtendingNonEntityWithInstanceGetEntityManager() {
		doTest(
				MapperSuperClassExtendingNonEntityWithInstanceGetEntityManager.class,
				EntityExtendingMapperSuperClassExtendingNonEntityWithInstanceGetEntityManager.class
		);
	}

	// NOTE: only this test matches the Panache use case exactly.
	// see https://github.com/quarkusio/quarkus/issues/38378#issuecomment-1911702314
	@Test
	@WithClasses({
			NonEntityWithStaticGetEntityManager.class,
			MapperSuperClassExtendingNonEntityWithStaticGetEntityManager.class,
			EntityExtendingMapperSuperClassExtendingNonEntityWithStaticGetEntityManager.class
	})
	public void mappedSuperClassExtendingNonEntityWithStaticGetEntityManager() {
		doTest(
				MapperSuperClassExtendingNonEntityWithStaticGetEntityManager.class,
				EntityExtendingMapperSuperClassExtendingNonEntityWithStaticGetEntityManager.class
		);
	}

	private void doTest(Class<?> superclass, Class<?> entitySubclass) {
		System.out.println( TestUtil.getMetaModelSourceAsString( superclass ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( entitySubclass ) );
		assertMetamodelClassGeneratedFor( entitySubclass );
		assertAbsenceOfNonDefaultConstructorInMetamodelFor(
				entitySubclass,
				"The generated metamodel shouldn't include a non-default constructor. In particular, it shouldn't be injected with an entity manager."
		);
	}
}

