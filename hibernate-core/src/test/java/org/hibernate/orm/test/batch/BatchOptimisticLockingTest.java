/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.persistence.RollbackException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class  BatchOptimisticLockingTest extends
		BaseNonConfigCoreFunctionalTestCase {

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Person.class,
		};
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, String.valueOf( 2 ) );
		settings.put( AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, Boolean.FALSE );
	}

	@Test
	public void testBatchAndOptimisticLocking() {
		doInHibernate( this::sessionFactory, session -> {
			Person person1 = new Person();
			person1.id = 1L;
			person1.name = "First";
			session.persist( person1 );

			Person person2 = new Person();
			person2.id = 2L;
			person2.name = "Second";
			session.persist( person2 );

			Person person3 = new Person();
			person3.id = 3L;
			person3.name = "Third";
			session.persist( person3 );

		} );

		try {
			inTransaction( session -> {
				List<Person> persons = session
						.createSelectionQuery( "select p from Person p", Person.class )
						.getResultList();

				for ( int i = 0; i < persons.size(); i++ ) {
					Person person = persons.get( i );
					person.name += " Person";

					if ( i == 1 ) {
						try {
							executorService.submit( () -> {
								doInHibernate( this::sessionFactory, _session -> {
									Person _person = _session.find( Person.class, person.id );
									_person.name += " Person is the new Boss!";
								} );
							} ).get();
						}
						catch (InterruptedException|ExecutionException e) {
							fail(e.getMessage());
						}
					}
				}
			} );
		}
		catch (Exception expected) {
			if ( getDialect() instanceof CockroachDialect ) {
				// CockroachDB always runs in SERIALIZABLE isolation, and uses SQL state 40001 to indicate
				// serialization failure. The failure is mapped to a RollbackException.
				assertEquals( RollbackException.class, expected.getClass() );
				var msg = "could not execute batch";
				assertEquals(
						msg,
						expected.getMessage().substring( 0, msg.length() )
				);
			}
			else {
				assertEquals( OptimisticLockException.class, expected.getClass() );

				if ( getDialect() instanceof MariaDBDialect && getDialect().getVersion().isAfter( 11, 6, 2 )) {
					assertTrue(
							expected.getMessage()
									.contains( "Record has changed since last read in table 'Person'" )
					);
				} else {
					assertTrue(
							expected.getMessage()
									.startsWith(
											"Batch update returned unexpected row count from update 1 (expected row count 1 but was 0) [update Person set name=?,version=? where id=? and version=?]" )
					);
				}
			}
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Version
		private long version;
	}
}
