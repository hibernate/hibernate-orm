/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9864")
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingWithJoinedTableMultiLevelInheritance
		extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( false, false );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Address.class,
				Person.class,
				SpecialPerson.class,
				AnotherPerson.class,
				President.class,
				Office.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void testBatchingAmongstSubClasses() {
		doInHibernate( this::sessionFactory, session -> {
			int iterations = 2;
			for ( int i = 0; i < iterations; i++ ) {
				final President president = new President();
				president.addAddress( new Address() );
				session.persist( president );

				final AnotherPerson anotherPerson = new AnotherPerson();
				Office office = new Office();
				session.persist( office );
				anotherPerson.office = office;
				session.persist( anotherPerson );

				final Person person = new Person();
				session.persist( person );

				final SpecialPerson specialPerson = new SpecialPerson();
				specialPerson.addAddress( new Address() );
				session.persist( specialPerson );
			}
			connectionProvider.clear();
		} );

		assertEquals( 17, connectionProvider.getPreparedStatements().size() );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void cleanupTestData() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete Address" ).executeUpdate();
			session.createQuery( "delete Person" ).executeUpdate();
			session.createQuery( "delete SpecialPerson" ).executeUpdate();
			session.createQuery( "delete AnotherPerson" ).executeUpdate();
			session.createQuery( "delete Office" ).executeUpdate();
			session.createQuery( "delete President" ).executeUpdate();
		} );
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;
	}

	@Entity(name = "Office")
	public static class Office {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID_2", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_2")
		private Long id;
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "CLASSINDICATOR", discriminatorType = DiscriminatorType.INTEGER)
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID_3", sequenceName = "PERSON_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_3")
		private Long id;

	}

	@Entity(name = "SpecialPerson")
	public static class SpecialPerson extends Person {
		@Column(name = "special")
		private String special;

		@OneToMany(orphanRemoval = true, cascade = {
				CascadeType.PERSIST,
				CascadeType.REMOVE
		})
		@JoinColumn(name = "PERSONID", referencedColumnName = "ID", nullable = false, updatable = false)
		@BatchSize(size = 100)
		private Set<Address> addresses = new HashSet<Address>();

		public void addAddress(Address address) {
			this.addresses.add( address );
		}
	}

	@Entity(name = "AnotherPerson")
	public static class AnotherPerson extends Person {
		private boolean working;

		@ManyToOne
		private Office office;
	}

	@Entity(name = "President")
	public static class President extends SpecialPerson {

		@Column(name = "salary")
		private BigDecimal salary;
	}
}
