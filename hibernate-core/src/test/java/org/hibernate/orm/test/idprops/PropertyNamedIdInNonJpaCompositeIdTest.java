/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idprops;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = PropertyNamedIdInNonJpaCompositeIdTest.Person.class
)
@SessionFactory
public class PropertyNamedIdInNonJpaCompositeIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( "John Doe", 0 ) );
			session.persist( new Person( "John Doe", 1 ) );
			session.persist( new Person( "Jane Doe", 0 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-13084")
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 2, session.createQuery( "from Person p where p.id = 0", Person.class ).list().size() );

			assertEquals( 3L, session.createQuery( "select count( p ) from Person p" ).uniqueResult() );

		} );
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@Id
		private String name;

		@Id
		private Integer id;

		public Person(String name, Integer id) {
			this();
			setName( name );
			setId( id );
		}

		public String getName() {
			return name;
		}

		public Integer getId() {
			return id;
		}

		protected Person() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setId(Integer id) {
			this.id = id;
		}
	}
}
