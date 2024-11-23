/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
public class InsertOrderingWithBidirectionalOneToMany extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Address.class, Person.class };
	}

	@Test
	public void testBatching() {
		doInHibernate( this::sessionFactory, session -> {
			Person father = new Person();
			Person mother = new Person();
			Person son = new Person();
			Person daughter = new Person();

			Address home = new Address();
			Address office = new Address();

			home.addPerson( father );
			home.addPerson( mother );
			home.addPerson( son );
			home.addPerson( daughter );

			office.addPerson( father );
			office.addPerson( mother );

			session.persist( home );
			session.persist( office );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Address (ID) values (?)", 2 ),
				new Batch( "insert into Person (address_ID, ID) values (?, ?)", 4 )
		);
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToMany(mappedBy = "address", cascade = CascadeType.PERSIST)
		private List<Person> persons = new ArrayList<>();

		public void addPerson(Person person) {
			persons.add( person );
			person.address = this;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@ManyToOne
		private Address address;
	}
}
