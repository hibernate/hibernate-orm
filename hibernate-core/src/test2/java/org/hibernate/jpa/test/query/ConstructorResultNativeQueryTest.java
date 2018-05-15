/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.SkipForDialect;

import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(value = Oracle8iDialect.class, jiraKey = "HHH-10323")
public class ConstructorResultNativeQueryTest extends BaseEntityManagerFunctionalTestCase {
	@Entity( name = "Person" )
	@SqlResultSetMappings(
			value = {
					@SqlResultSetMapping(
							name = "person-id-and-name",
							classes = {
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id" ),
													@ColumnResult( name = "p_name" )
											}
									)
							}
					),
					@SqlResultSetMapping(
							name = "person-id-and-name2",
							classes = {
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id" ),
													@ColumnResult( name = "p_name" )
											}
									),
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id2" ),
													@ColumnResult( name = "p_name2" )
											}
									)
							}
					),
					@SqlResultSetMapping(
							name = "person-id-and-name-and-weight",
							classes = {
									@ConstructorResult(
											targetClass = Person.class,
											columns = {
													@ColumnResult( name = "id" ),
													@ColumnResult( name = "p_name" ),
													@ColumnResult( name = "p_weight", type=String.class )
											}
									)
							}
					)
			}
	)
	@NamedNativeQueries(
			value = {
				@NamedNativeQuery(
						name = "person-id-and-name",
						query = "select p.id, p.p_name from person p order by p.p_name",
						resultSetMapping = "person-id-and-name"
				),
				@NamedNativeQuery(
						name = "person-id-and-name2",
						query = "select p.id, p.p_name, p.id as id2, p.p_name as p_name2 from person p order by p.p_name",
						resultSetMapping = "person-id-and-name2"
				),
				@NamedNativeQuery(
						name = "person-id-and-name-and-weight",
						query = "select p.id, p.p_name, p.p_weight from person p order by p.p_name",
						resultSetMapping = "person-id-and-name-and-weight"
				)
			}
	)
	@Table(name = "person")
	public static class Person {
		@Id
		@Column(name = "id")
		private Integer id;
		@Column( name = "p_name" )
		private String name;
		@Temporal( TemporalType.TIMESTAMP )
		private Date birthDate;
		@Column( name = "p_weight" )
		private int weight;

		public Person() {
		}

		public Person(Integer id, String name, Date birthDate) {
			this.id = id;
			this.name = name;
			this.birthDate = birthDate;
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Person(Integer id, String name, String weight) {
			this.id = id;
			this.name = name;
			this.weight = Integer.valueOf(weight);
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}


	@Test
	public void testConstructorResultNativeQuery() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Person( 1, "John", new Date() ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List results = em.createNativeQuery(
				"select p.id, p.p_name from person p order by p.p_name",
				"person-id-and-name"
		).getResultList();
		assertEquals( 1, results.size() );
		assertTyping( Person.class, results.get( 0 ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from Person" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}


	@Test
	public void testMultipleConstructorResultNativeQuery() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Person( 1, "John", new Date() ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List results = em.createNamedQuery( "person-id-and-name2" ).getResultList();
		assertEquals( 1, results.size() );
		Object[] result = assertTyping( Object[].class, results.get( 0 ) );
		assertEquals( 2, result.length );
		assertTyping( Person.class, result[0] );
		assertTyping( Person.class, result[1] );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from Person" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testConstructorResultNativeQuerySpecifyingType() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Person( 1, "John", "85" ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List results = em.createNamedQuery( "person-id-and-name-and-weight" ).getResultList();
		assertEquals( 1, results.size() );
		assertTyping( Person.class, results.get( 0 ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from Person" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}
}
