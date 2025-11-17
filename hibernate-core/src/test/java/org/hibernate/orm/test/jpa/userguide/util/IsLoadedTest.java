/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Author.class,
		Book.class,
		CopyrightableContent.class
})
public class IsLoadedTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testIsLoadedOnPrivateSuperclassProperty(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Author a = new Author();
			Book book = new Book( a );
			entityManager.persist( a );
			entityManager.persist( book );
			entityManager.flush();
			entityManager.clear();
			book = entityManager.find( Book.class, book.getId() );
			assertTrue( entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded( book ) );
			assertFalse( entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded( book, "author" ) );
		} );
	}

}
