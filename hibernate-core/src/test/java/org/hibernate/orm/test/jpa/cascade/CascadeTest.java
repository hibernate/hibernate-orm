/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Max Rydahl Andersen
 */
@Jpa(annotatedClasses = {
		Teacher.class,
		Student.class,
		Song.class,
		Author.class
})
public class CascadeTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCascade(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Teacher teacher = new Teacher();
					Student student = new Student();

					teacher.setFavoriteStudent( student );
					student.setFavoriteTeacher( teacher );

					teacher.getStudents().add( student );
					student.setPrimaryTeacher( teacher );

					entityManager.persist( teacher );
					entityManager.getTransaction().commit();

					entityManager.getTransaction().begin();
					Teacher foundTeacher = (Teacher) entityManager.createQuery( "select t from Teacher as t" )
							.getSingleResult();

					System.out.println( foundTeacher );
					System.out.println( foundTeacher.getFavoriteStudent() );

					for ( Student fstudent : foundTeacher.getStudents() ) {
						System.out.println( fstudent );
						System.out.println( fstudent.getFavoriteTeacher() );
						System.out.println( fstudent.getPrimaryTeacher() );
					}
				}
		);
	}

	@Test
	public void testNoCascadeAndMerge(EntityManagerFactoryScope scope) {
		Song s1 = new Song();
		Author a1 = new Author();
		s1.setAuthor( a1 );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( a1 );
					entityManager.persist( s1 );
				}
		);

		Song s2 = scope.fromTransaction(
				entityManager -> entityManager.find( Song.class, s1.getId() )
		);

		scope.inTransaction(
				entityManager -> entityManager.merge( s2 )
		);
	}
}
