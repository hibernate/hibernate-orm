/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec;

import java.time.Instant;
import java.util.Date;

import org.hibernate.orm.test.metamodel.mapping.SecondaryTableTests;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT,
		annotatedClasses = SecondaryTableTests.SimpleEntityWithSecondaryTables.class
)
@ServiceRegistry
@SessionFactory( exportSchema = true )
public class HqlDeleteExecutionTests {
	@Test
	public void testSimpleDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity" ).executeUpdate()
		);
	}

	@Test
	public void testSimpleRestrictedDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity where data = :filter" )
						.setParameter( "filter", "abc" )
						.executeUpdate()
		);
	}

	@Test
	public void testSimpleMultiTableDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete SimpleEntityWithSecondaryTables" )
						.executeUpdate()
		);
	}

	@Test
	public void testSimpleMultiTableRestrictedDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete SimpleEntityWithSecondaryTables where data = :filter" )
						.setParameter( "filter", "abc" )
						.executeUpdate()
		);
	}

	@Test
	@FailureExpected( reason = "Saving of entities with secondary tables is broken atm" )
	public void testSimpleMultiTableRestrictedDeleteResults(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.save(
							new SecondaryTableTests.SimpleEntityWithSecondaryTables(
									1,
									"first",
									Date.from( Instant.now() ),
									"1 - cfdjdjvokfobkofbvovoijjbvoijofjdbiof"
							)
					);
					session.save(
							new SecondaryTableTests.SimpleEntityWithSecondaryTables(
									2,
									"second",
									Date.from( Instant.now() ),
									"2 - s3o2rj9 fcojv9j gj9jfv943jv29j9j4"
							)
					);
					session.save(
							new SecondaryTableTests.SimpleEntityWithSecondaryTables(
									3,
									"third",
									Date.from( Instant.now() ),
									"abc"
							)
					);
				}
		);
		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete SimpleEntityWithSecondaryTables where data = :filter" )
							.setParameter( "filter", "abc" )
							.executeUpdate();
					assertThat( rows, is ( 1 ) );
				}
		);
		scope.inTransaction(
				session -> session.createQuery( "delete SimpleEntityWithSecondaryTables" ).executeUpdate()
		);
	}

}
