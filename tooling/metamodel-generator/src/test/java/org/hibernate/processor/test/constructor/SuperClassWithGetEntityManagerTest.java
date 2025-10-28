/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.constructor;

import static org.hibernate.processor.test.util.TestUtil.assertAbsenceOfNonDefaultConstructorInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;

/**
 * Test various scenarios where a superclass of an entity has a {@code getEntityManager()} method.
 * <p>
 * The superclass may be itself an entity, or a mapped superclass, or not mapped at all.
 * <p>
 * The method may be static or not.
 */
@CompilationTest
@TestForIssue(jiraKey = "HHH-17683")
class SuperClassWithGetEntityManagerTest {
	@Test
	@WithClasses({ EntityWithInstanceGetEntityManager.class, EntityExtendingEntityWithInstanceGetEntityManager.class })
	void entityWithInstanceGetEntityManager() {
		doTest( EntityWithInstanceGetEntityManager.class, EntityExtendingEntityWithInstanceGetEntityManager.class );
	}

	@Test
	@WithClasses({ EntityWithStaticGetEntityManager.class, EntityExtendingEntityWithStaticGetEntityManager.class })
	void entityWithStaticGetEntityManager() {
		doTest( EntityWithStaticGetEntityManager.class, EntityExtendingEntityWithStaticGetEntityManager.class );
	}

	@Test
	@WithClasses({
			NonEntityWithInstanceGetEntityManager.class, EntityExtendingNonEntityWithInstanceGetEntityManager.class
	})
	void nonEntityWithInstanceGetEntityManager() {
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
	void nonEntityWithStaticGetEntityManager() {
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
	void mappedSuperClassWithInstanceGetEntityManager() {
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
	void mappedSuperClassWithStaticGetEntityManager() {
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
	void mappedSuperClassExtendingNonEntityWithInstanceGetEntityManager() {
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
	void mappedSuperClassExtendingNonEntityWithStaticGetEntityManager() {
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
