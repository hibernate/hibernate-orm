/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9864")
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingWithBidirectionalMapsIdOneToOne extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Address.class, Person.class };
	}

	@Test
	public void testBatching() {
		doInHibernate( this::sessionFactory, session -> {
			Person worker = new Person();
			Person homestay = new Person();

			Address home = new Address();
			Address office = new Address();

			home.addPerson( homestay );

			office.addPerson( worker );

			session.persist( home );
			session.persist( office );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Address (ID) values (?)", 2 ),
				new Batch( "insert into Person (address_ID) values (?)", 2 )
		);
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToOne(mappedBy = "address", cascade = CascadeType.PERSIST)
		private Person person;

		public void addPerson(Person person) {
			this.person = person;
			person.address = this;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		private Address address;
	}
}
