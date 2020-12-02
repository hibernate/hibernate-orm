/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Max Rydahl Andersen
 */
@DomainModel(annotatedClasses = {
		Teacher.class,
		Student.class,
		Song.class,
		Author.class
})
@SessionFactory
public class CascadeTest {
	@Test
	public void testCascade(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Teacher teacher = new Teacher();
					Student student = new Student();

					teacher.setFavoriteStudent( student );
					student.setFavoriteTeacher( teacher );

					teacher.getStudents().add( student );
					student.setPrimaryTeacher( teacher );

					session.persist( teacher );
				}
		);

		scope.inTransaction(
				session -> {
					Teacher foundTeacher = (Teacher) session.createQuery( "select t from Teacher as t" )
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
	public void testNoCascadeAndMerge(SessionFactoryScope scope) {
		Song s1 = new Song();
		Author a1 = new Author();
		s1.setAuthor( a1 );

		scope.inTransaction(
				session -> {
					session.persist( a1 );
					session.persist( s1 );
				}
		);

		Song s2 = scope.fromSession(
				session -> session.find( Song.class, s1.getId() )
		);

		scope.inTransaction(
				session -> session.merge( s2 )
		);
	}
}
