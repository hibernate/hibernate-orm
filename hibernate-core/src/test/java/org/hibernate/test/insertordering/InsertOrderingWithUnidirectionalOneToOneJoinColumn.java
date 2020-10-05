/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Query;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12569")
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingWithUnidirectionalOneToOneJoinColumn extends BaseInsertOrderingTest  {
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

	@Test
	public void testBatchingWithEmbeddableId() {
		doInHibernate( this::sessionFactory, session -> {
			final PersonAddressId id = new PersonAddressId();
			id.setId( 1 );

			Person person = new Person();
			person.setId( id );
			session.persist( person );

			Address address = new Address();
			address.setId( id );
			address.setPerson( person );
			session.persist( address );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Person (id) values (?)" ),
				new Batch( "insert into Address (id) values (?)" )
		);

		doInHibernate( this::sessionFactory, session -> {
			Query query = session.createQuery( "FROM Person" );
			assertEquals( 1, query.getResultList().size() );
		} );
	}
}
