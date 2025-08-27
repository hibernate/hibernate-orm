/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.hhh16711;

import java.util.List;

import org.hibernate.orm.test.inheritance.hhh16711.otherPackage.Inherited;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				Inheriting.class,
				Inherited.class
		}
)
public class MappedSuperclassInheritanceTest {

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-16707")
	public void testNew(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Inheriting inheriting = new Inheriting("myId", "myName");
			entityManager.persist( inheriting );
		});

		scope.inTransaction(entityManager -> {
			List<Object[]> results = entityManager.createQuery( "SELECT i.id, i.name FROM Inheriting i", Object[].class).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( "myId", results.get(0)[0].toString() );
			assertEquals( "myName", results.get(0)[1].toString() );
		});
	}

	@Test
	@JiraKey( value = "HHH-16711")
	public void testSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			entityManager.createNativeQuery("INSERT INTO inheriting VALUES ('myId', 'myName')").executeUpdate();
		});

		scope.inTransaction(entityManager -> {
			List<Inheriting> results = entityManager.createQuery( "SELECT i FROM Inheriting i", Inheriting.class).getResultList();
			assertEquals( 1, results.size() );
		});
	}

}
