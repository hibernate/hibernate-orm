/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec;

import java.time.Instant;
import java.util.Date;

import org.hibernate.orm.test.mapping.SecondaryTableTests;
import org.hibernate.orm.test.mapping.inheritance.joined.JoinedInheritanceTest;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT,
		annotatedClasses = {
				SecondaryTableTests.SimpleEntityWithSecondaryTables.class,
				JoinedInheritanceTest.Customer.class,
				JoinedInheritanceTest.DomesticCustomer.class,
				JoinedInheritanceTest.ForeignCustomer.class
		}
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
	public void testSimpleMultiTableRestrictedDeleteResults(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(
							new SecondaryTableTests.SimpleEntityWithSecondaryTables(
									1,
									"first",
									Date.from( Instant.now() ),
									"1 - cfdjdjvokfobkofbvovoijjbvoijofjdbiof"
							)
					);
					session.persist(
							new SecondaryTableTests.SimpleEntityWithSecondaryTables(
									2,
									"second",
									Date.from( Instant.now() ),
									"2 - s3o2rj9 fcojv9j gj9jfv943jv29j9j4"
							)
					);
					session.persist(
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


	@Test
	public void testJoinedSubclassRootDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete Customer" ).executeUpdate()
		);
	}

	@Test
	public void testJoinedSubclassRootRestrictedDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete Customer where name = 'abc'" ).executeUpdate()
		);
	}

	@Test
	public void testJoinedSubclassRootRestrictedDeleteResults(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(
							new JoinedInheritanceTest.ForeignCustomer( 1, "Adventures Abroad", "123" )
					);
					session.persist(
							new JoinedInheritanceTest.DomesticCustomer( 2, "Domestic Wonders", "456" )
					);
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete Customer where name = 'Adventures Abroad'" ).executeUpdate();
					assertThat( rows, is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete from Customer" ).executeUpdate();
					assertThat( rows, is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete from Customer" ).executeUpdate();
					assertThat( rows, is( 0 ) );
				}
		);
	}


	@Test
	public void testJoinedSubclassLeafDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete ForeignCustomer" ).executeUpdate()
		);
		scope.inTransaction(
				session -> session.createQuery( "delete DomesticCustomer" ).executeUpdate()
		);
	}

	@Test
	public void testJoinedSubclassLeafRestrictedDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete ForeignCustomer where name = 'abc'" ).executeUpdate()
		);
		scope.inTransaction(
				session -> session.createQuery( "delete DomesticCustomer where name = 'abc'" ).executeUpdate()
		);
	}

	@Test
	public void testJoinedSubclassLeafRestrictedDeleteResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(
							new JoinedInheritanceTest.ForeignCustomer( 1, "Adventures Abroad", "123" )
					);
					session.persist(
							new JoinedInheritanceTest.DomesticCustomer( 2, "Domestic Wonders", "456" )
					);
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete ForeignCustomer where name = 'Adventures Abroad'" )
							.executeUpdate();
					assertThat( rows, is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete DomesticCustomer where name = 'Domestic Wonders'" )
							.executeUpdate();
					assertThat( rows, is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete Customer" )
							.executeUpdate();
					assertThat( rows, is( 0 ) );
				}
		);
	}

}
