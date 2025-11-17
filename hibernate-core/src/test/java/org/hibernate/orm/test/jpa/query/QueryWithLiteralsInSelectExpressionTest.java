/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Andrea Boriero
 */
@Jpa(annotatedClasses = {QueryWithLiteralsInSelectExpressionTest.MyEntity.class})
public class QueryWithLiteralsInSelectExpressionTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new MyEntity( "Fab", "A" ) ) );
	}

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-10230")
	public void testSelectLiterals(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final List<Object[]> elements = entityManager.createQuery(
					"SELECT true, false, e.name FROM MyEntity e",
					Object[].class
			).getResultList();
			assertEquals( 1, elements.size() );
			assertEquals( 3, elements.get( 0 ).length );
			assertEquals( true, elements.get( 0 )[0] );
			assertEquals( false, elements.get( 0 )[1] );
			assertEquals( "Fab", elements.get( 0 )[2] );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10230")
	public void testSelectNonNullLiteralsCastToBoolean(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final List<Object[]> elements = entityManager.createQuery(
					"SELECT cast( true as boolean ), cast( false as boolean ), e.name FROM MyEntity e",
					Object[].class
			).getResultList();
			assertEquals( 1, elements.size() );
			assertEquals( 3, elements.get( 0 ).length );
			assertEquals( true, elements.get( 0 )[ 0 ] );
			assertEquals( false, elements.get( 0 )[ 1 ] );
			assertEquals( "Fab", elements.get( 0 )[ 2 ] );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10230")
	public void testSelectNullLiterals(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final List<Object[]> elements = entityManager.createQuery(
					"SELECT cast(null as boolean), false, e.name FROM MyEntity e",
					Object[].class
			).getResultList();
			assertEquals( 1, elements.size() );
			assertEquals( 3, elements.get( 0 ).length );
			assertNull( elements.get( 0 )[0] );
			assertEquals( false, elements.get( 0 )[ 1 ] );
			assertEquals( "Fab", elements.get( 0 )[ 2 ] );
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity implements Serializable {
		@Id
		@Column(name = "id")
		@GeneratedValue
		private Integer id;
		private String name;
		private String surname;

		public MyEntity() {
		}

		public MyEntity(String name, String surname) {
			this.name = name;
			this.surname = surname;
		}

		public MyEntity(String name, boolean surname) {
			this.name = name;
		}
	}

	public static class MyEntityDTO {
		private String name;
		private String surname;
		private boolean active;

		public MyEntityDTO() {
		}

		public MyEntityDTO(String name, String surname) {
			this.name = name;
			this.surname = surname;
		}

		public MyEntityDTO(String name, boolean active) {
			this.name = name;
			this.active = active;
		}
	}

}
