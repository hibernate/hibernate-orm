/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OptimisticLockException;
import javax.persistence.Version;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class BatchOptimisticLockingTest extends
		BaseNonConfigCoreFunctionalTestCase {

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Person.class,
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, String.valueOf( 2 ) );
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
			doInHibernate( this::sessionFactory, session -> {
				List<Person> persons = session.createQuery( "select p from Person p").getResultList();

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
			assertEquals( OptimisticLockException.class, expected.getClass());
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

