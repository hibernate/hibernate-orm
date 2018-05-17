/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12569")
public class InsertOrderingWithUnidirectionalOneToOneJoinColumn extends BaseEntityManagerFunctionalTestCase  {
	@Embeddable
	public static class PersonAddressId implements Serializable {
		@Column(name = "id")
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@EmbeddedId
		private PersonAddressId id;

		public PersonAddressId getId() {
			return id;
		}

		public void setId(PersonAddressId id) {
			this.id = id;
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@EmbeddedId
		private PersonAddressId id;
		@OneToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
		private Person person;

		public PersonAddressId getId() {
			return id;
		}

		public void setId(PersonAddressId id) {
			this.id = id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Address.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.ORDER_INSERTS, "true" );
	}

	@Test
	public void testBatchingWithEmbeddableId() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final PersonAddressId id = new PersonAddressId();
			id.setId( 1 );

			Person person = new Person();
			person.setId( id );
			entityManager.persist( person );

			Address address = new Address();
			address.setId( id );
			address.setPerson( person );
			entityManager.persist( address );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Query query = entityManager.createQuery( "FROM Person" );
			assertEquals( 1, query.getResultList().size() );
		} );
	}
}
