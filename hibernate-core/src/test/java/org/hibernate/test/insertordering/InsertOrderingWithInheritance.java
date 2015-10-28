/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-9864" )
public class InsertOrderingWithInheritance extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	@FailureExpected( jiraKey = "HHH-9864" )
	public void testBatchOrdering() {
		Session session = openSession();
		session.getTransaction().begin();

		// First object with dependent object (address)
		final Person person = new Person();
		person.addAddress(new Address());
		session.persist(person);

		// Derived Object with dependent object (address)
		final SpecialPerson specialPerson = new SpecialPerson();
		specialPerson.addAddress(new Address());
		session.persist( specialPerson );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9864" )
	public void testBatchingAmongstSubClasses() {
		StatsBatch.reset();
		Session session = openSession();
		session.getTransaction().begin();
		int iterations = 12;
		for ( int i = 0; i < iterations; i++ ) {
			final Person person = new Person();
			person.addAddress( new Address() );
			session.persist( person );

			final SpecialPerson specialPerson = new SpecialPerson();
			specialPerson.addAddress(new Address());
			session.persist( specialPerson );
		}
		StatsBatch.reset();
		session.getTransaction().commit();
		session.close();

		// 2 batches of Person
		// 2 batches of SpecialPerson
		// 2 batches of Address
		assertEquals( 6, StatsBatch.batchSizes.size() );

		cleanupData();
	}

	private void cleanupData() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Address" ).executeUpdate();
		session.createQuery( "delete Person" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class, Person.class, SpecialPerson.class };
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put( BatchBuilderInitiator.BUILDER, StatsBatchBuilder.class.getName() );
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS")
	@Access(AccessType.FIELD)
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;
	}

	@Entity( name = "Person" )
	@Access(AccessType.FIELD)
	@Table(name = "PERSON")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "CLASSINDICATOR", discriminatorType = DiscriminatorType.INTEGER)
	@DiscriminatorValue("1")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "PERSON_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToMany(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
		@JoinColumn(name = "PERSONID", referencedColumnName = "ID", nullable = false, updatable = false)
		@BatchSize(size = 100)
		private Set<Address> addresses = new HashSet<Address>();

		public void addAddress(Address address) {
			this.addresses.add(address);
		}

	}

	@Entity
	@Access(AccessType.FIELD)
	@DiscriminatorValue("2")
	public static class SpecialPerson extends Person {
		@Column(name = "special")
		private String special;
	}

	public static class Counter {
		public int count = 0;
	}

	public static class StatsBatch extends BatchingBatch {
		private static String batchSQL;
		private static List batchSizes = new ArrayList();
		private static int currentBatch = -1;

		public StatsBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int jdbcBatchSize) {
			super( key, jdbcCoordinator, jdbcBatchSize );
		}

		static void reset() {
			batchSizes = new ArrayList();
			currentBatch = -1;
			batchSQL = null;
		}

		@Override
		public PreparedStatement getBatchStatement(String sql, boolean callable) {
			if ( batchSQL == null || ! batchSQL.equals( sql ) ) {
				currentBatch++;
				batchSQL = sql;
				batchSizes.add( currentBatch, new Counter() );
			}
			return super.getBatchStatement( sql, callable );
		}

		@Override
		public void addToBatch() {
			Counter counter = ( Counter ) batchSizes.get( currentBatch );
			counter.count++;
			super.addToBatch();
		}
	}

	public static class StatsBatchBuilder extends BatchBuilderImpl {
		private int jdbcBatchSize;

		@Override
		public void setJdbcBatchSize(int jdbcBatchSize) {
			this.jdbcBatchSize = jdbcBatchSize;
		}

		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return new StatsBatch( key, jdbcCoordinator, jdbcBatchSize );
		}
	}
}
