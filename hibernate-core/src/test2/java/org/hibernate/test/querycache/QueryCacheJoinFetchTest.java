/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.QueryHints;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12430" )
public class QueryCacheJoinFetchTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.USE_QUERY_CACHE, "true" );
		options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		options.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12430" )
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			Phone phone1 = new Phone( "123-456-7890" );
			Phone phone2 = new Phone( "321-654-0987" );

			person.addPhone( phone1 );
			person.addPhone( phone2 );
			entityManager.persist( person );
		} );

		entityManagerFactory().getCache().evictAll();
		entityManagerFactory().unwrap( SessionFactory.class ).getStatistics().clear();

		Person person = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
				"select distinct p " +
				"from Person p " +
				"join fetch p.phones ph", Person.class )
			.setHint( QueryHints.CACHEABLE, Boolean.TRUE )
			.getSingleResult();
		} );

		assertEquals( 2, person.getPhones().size() );
		assertEquals(
				0,
				entityManagerFactory().unwrap( SessionFactory.class ).getStatistics().getQueryCacheHitCount()
		);

		person = doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.getEntityManagerFactory().getCache().evictAll();

			return entityManager.createQuery(
				"select distinct p " +
				"from Person p " +
				"join fetch p.phones ph", Person.class )
			.setHint( QueryHints.CACHEABLE, Boolean.TRUE )
			.getSingleResult();
		} );

		assertEquals(
				1,
				entityManagerFactory().unwrap( SessionFactory.class ).getStatistics().getQueryCacheHitCount()
		);

		assertEquals( 2, person.getPhones().size() );
	}

	@Entity(name = "Person")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Phone> phones = new ArrayList<>();

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}

		public void removePhone(Phone phone) {
			phones.remove( phone );
			phone.setPerson( null );
		}
	}

	@Entity(name = "Phone")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		@Column(name = "`number`", unique = true)
		private String number;

		@ManyToOne
		private Person person;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Phone phone = (Phone) o;
			return Objects.equals( number, phone.number );
		}

		@Override
		public int hashCode() {
			return Objects.hash( number );
		}
	}
}
