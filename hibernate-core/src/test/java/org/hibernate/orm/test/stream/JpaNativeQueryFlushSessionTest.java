/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		scope.getEntityManagerFactory().getSchemaManager().truncate();
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
