/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Andrea Boriero
 */
public class QueryWithLiteralsInSelectExpressionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class};
	}

	@Before
	public void init() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.persist( new MyEntity( "Fab", "A" ) );
			entityManager.getTransaction().commit();
		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10230")
	public void testSelectLiterals() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			final List<Object[]> elements = entityManager.createQuery(
					"SELECT true, false, e.name FROM MyEntity e",
					Object[].class
			).getResultList();
			Assert.assertEquals( 1, elements.size() );
			Assert.assertEquals( 3, elements.get( 0 ).length );
			Assert.assertEquals( true, elements.get( 0 )[0] );
			Assert.assertEquals( false, elements.get( 0 )[1] );
			Assert.assertEquals( "Fab", elements.get( 0 )[2] );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10230")
	public void testSelectNonNullLiteralsCastToBoolean() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			final List<Object[]> elements = entityManager.createQuery(
					"SELECT cast( true as boolean ), cast( false as boolean ), e.name FROM MyEntity e",
					Object[].class
			).getResultList();
			Assert.assertEquals( 1, elements.size() );
			Assert.assertEquals( 3, elements.get( 0 ).length );
			Assert.assertEquals( true, elements.get( 0 )[ 0 ] );
			Assert.assertEquals( false, elements.get( 0 )[ 1 ] );
			Assert.assertEquals( "Fab", elements.get( 0 )[ 2 ] );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10230")
	public void testSelectNullLiterals() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			final List<Object[]> elements = entityManager.createQuery(
					"SELECT cast(null as boolean), false, e.name FROM MyEntity e",
					Object[].class
			).getResultList();
			Assert.assertEquals( 1, elements.size() );
			Assert.assertEquals( 3, elements.get( 0 ).length );
			Assert.assertEquals( null, elements.get( 0 )[ 0 ] );
			Assert.assertEquals( false, elements.get( 0 )[ 1 ] );
			Assert.assertEquals( "Fab", elements.get( 0 )[ 2 ] );
		}
		finally {
			entityManager.close();
		}
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
