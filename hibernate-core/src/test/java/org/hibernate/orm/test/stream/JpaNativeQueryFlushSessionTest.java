/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.stream;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				JpaNativeQueryFlushSessionTest.Person.class
		}
)
@JiraKey(value = "HHH-16492")
public class JpaNativeQueryFlushSessionTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Person " ).executeUpdate();
				}
		);
	}

	@Test
	public void testSessionIsFlushedWhenNativeQueryIsExecuted(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<String> resultList = entityManager
							.createNativeQuery( "select name from Person where name ='John Doe'" )
							.getResultList();
					assertThat( resultList.size() ).isEqualTo( 0 );

					Person person = new Person( 1l, "John Doe" );
					entityManager.persist( person );

					try (Stream<String> resultStream = entityManager
							.createNativeQuery( "select name from Person where name ='John Doe'" )
							.getResultStream()) {
						List<String> results = resultStream.collect( Collectors.toList() );

						assertThat( results.size() ).isEqualTo( 1 );
					}
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
