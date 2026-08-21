/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				OptionalSelectClauseWithJPAQLStrictComplinceTest.QueryBook.class
		},
		properties = @Setting(name = AvailableSettings.JPAQL_STRICT_COMPLIANCE, value = "true")
)
@JiraKey("HHH-20423")
public class OptionalSelectClauseWithJPAQLStrictComplinceTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					QueryBook book1 = new QueryBook( 1, "Book 1" );
					QueryBook book2 = new QueryBook( 2, "Book 2" );
					QueryBook book3 = new QueryBook( 3, "Book 3" );
					entityManager.persist( book1 );
					entityManager.persist( book2 );
					entityManager.persist( book3 );
				}
		);
	}

	@Test
	public void optionalSelectClausesTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<QueryBook> books = entityManager
							.createQuery( "FROM QueryBook WHERE title = :title", QueryBook.class )
							.setParameter( "title", "Book 1" )
							.getResultList();
					assertThat( books.size() ).isEqualTo( 1 );
					assertThat( books.get( 0 ).getId() ).isEqualTo( 1 );
				}
		);

	}

	@Entity(name = "QueryBook")
	public static class QueryBook {

		@Id
		private Integer id;

		private String title;


		public QueryBook() {
		}

		public QueryBook(Integer id, String title) {
			this.id = id;
			this.title = title;
		}

		public Integer getId() {
			return id;
		}
	}
}
