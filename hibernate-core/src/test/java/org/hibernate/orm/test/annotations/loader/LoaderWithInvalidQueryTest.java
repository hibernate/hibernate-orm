/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.loader;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				LoaderWithInvalidQueryTest.Person.class
		}
)
public class LoaderWithInvalidQueryTest {


	@Test
	public void test(EntityManagerFactoryScope scope) {
		Exception expected = assertThrows( Exception.class, () -> scope.inTransaction(
				session -> {
					fail("Exception expected during the build of the EntityManagerFactory ");
				}
		) );

		HibernateException rootCause = (HibernateException) ExceptionUtil.rootCause( expected );
		Throwable[] suppressed = rootCause.getSuppressed();
		assertThat( suppressed.length ).isEqualTo( 2 );
		assertThat( ExceptionUtil.rootCause( suppressed[0] ).getMessage() )
				.contains( "Could not resolve root entity '_Person'" );
		assertThat( ExceptionUtil.rootCause( suppressed[1] ).getMessage() )
				.contains( "Could not resolve attribute 'valid'" );
	}


	@Entity(name = "Person")
	@HQLSelect(
			query = "SELECT p " +
					"FROM Person p " +
					"WHERE p.id = ?1 and p.valid = true"
	)
	@NamedQuery(
			name = "another_invalid_sql",
			query = "SELECT p " +
					"FROM _Person p " +
					"WHERE p.id = ?1"
	)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "full_name")
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	//end::sql-custom-crud-example[]

}
