/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import jakarta.persistence.Version;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.getDialect;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				BatchOptimisticLockingTest.Person.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2"),
				@Setting(name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false")
		}
)
public class BatchOptimisticLockingTest {

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();


	@Test
	public void testBatchAndOptimisticLocking(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

		Exception exception = assertThrows( Exception.class, () -> scope.inTransaction( session -> {
			List<Person> persons = session
					.createSelectionQuery( "select p from Person p", Person.class )
					.getResultList();

			for ( int i = 0; i < persons.size(); i++ ) {
				Person person = persons.get( i );
				person.name += " Person";

				if ( i == 1 ) {
					try {
						executorService.submit( () -> {
							scope.inTransaction( _session -> {
								Person _person = _session.find( Person.class, person.id );
								_person.name += " Person is the new Boss!";
							} );
						} ).get();
					}
					catch (InterruptedException | ExecutionException e) {
						fail( e.getMessage() );
					}
				}
			}
		} ) );

		Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		if ( dialect instanceof CockroachDialect ) {
			// CockroachDB always runs in SERIALIZABLE isolation, and uses SQL state 40001 to indicate
			// serialization failure. The failure is mapped to a RollbackException.
			assertThat( exception ).isInstanceOf( RollbackException.class );
			var msg = "could not execute batch";
			assertThat( exception.getMessage() ).contains( msg );
		}
		else {
			assertThat( exception ).isInstanceOf( OptimisticLockException.class );

			if ( dialect instanceof MariaDBDialect && getDialect().getVersion().isAfter( 11, 6, 2 ) ) {
				assertThat( exception.getMessage() ).contains( "Record has changed since last read in table 'Person'" );

			}
			else {
				assertThat( exception.getMessage() ).startsWith(
						"Batch update returned unexpected row count from update 1 (expected row count 1 but was 0) [update Person set name=?,version=? where id=? and version=?]" );
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
