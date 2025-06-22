/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec;

import org.hibernate.orm.test.mapping.SecondaryTableTests;
import org.hibernate.orm.test.mapping.inheritance.joined.JoinedInheritanceTest;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
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
public class HqlUpdateExecutionTests {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// single table

	@Test
	public void testSimpleUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "update BasicEntity set data = :p" )
						.setParameter( "p", "xyz" )
						.executeUpdate()
		);
	}

	@Test
	@JiraKey( value = "HHH-15361")
	public void testSimpleUpdateAssignability(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "update EntityOfBasics set theDate = current_date" )
						.executeUpdate()
		);
	}

	@Test
	@JiraKey( value = "HHH-15939" )
	public void testNumericUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "update EntityOfBasics set theShort = 69" )
					.executeUpdate();
			session.createMutationQuery( "update EntityOfBasics set theShort = ?1" )
					.setParameter( 1, 69 )
					.executeUpdate();
		} );
	}

	@Test
	public void testSimpleUpdateWithData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new BasicEntity( 1, "abc" ) );
					session.persist( new BasicEntity( 2, "def" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "update BasicEntity set data = :p" )
							.setParameter( "p", "xyz" )
							.executeUpdate();
					assertThat( rows, is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final BasicEntity basicEntity = session.get( BasicEntity.class, 1 );
					assertThat( basicEntity.getData(), is( "xyz" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete BasicEntity" ).executeUpdate();
					assertThat( rows, is( 2 ) );
				}
		);
	}

	@Test
	public void testSimpleRestrictedUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "update BasicEntity set data = :p where data = :filter" )
						.setParameter( "p", "xyz" )
						.setParameter( "filter", "abc" )
						.executeUpdate()
		);
	}

	@Test
	public void testSimpleRestrictedUpdateWithData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new BasicEntity( 1, "abc" ) );
					session.persist( new BasicEntity( 2, "def" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "update BasicEntity set data = :val where data = :filter" )
							.setParameter( "val", "xyz" )
							.setParameter( "filter", "abc" )
							.executeUpdate();
					assertThat( rows, is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final BasicEntity basicEntity = session.get( BasicEntity.class, 1 );
					assertThat( basicEntity.getData(), is( "xyz" ) );

					final BasicEntity basicEntity2 = session.get( BasicEntity.class, 2 );
					assertThat( basicEntity2.getData(), is( "def" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final int rows = session.createQuery( "delete BasicEntity" ).executeUpdate();
					assertThat( rows, is( 2 ) );
				}
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// secondary tables

	@Test
	public void testSecondaryTableUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "update SimpleEntityWithSecondaryTables set name = :p" )
						.setParameter( "p", "xyz" )
						.executeUpdate()
		);
	}

	@Test
	public void testSecondaryTableRestrictedUpdatePrimary(SessionFactoryScope scope) {
		// attempts to update the entity referring to just columns in the root table
		scope.inTransaction(
				session -> session.createQuery( "update SimpleEntityWithSecondaryTables set name = :p where name = :x" )
						.setParameter( "p", "xyz" )
						.setParameter( "x", "abc" )
						.executeUpdate()
		);
	}

	@Test
	public void testSecondaryTableRestrictedUpdate(SessionFactoryScope scope) {
		// attempts to update the entity referring to columns in non-root table
		scope.inTransaction(
				session -> session.createQuery( "update SimpleEntityWithSecondaryTables set name = :p where data = :x" )
						.setParameter( "p", "xyz" )
						.setParameter( "x", "123" )
						.executeUpdate()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// joined subclassing

	@Test
	public void testJoinedSubclassRootUpdateUnrestricted(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "update Customer set name = :n" )
						.setParameter( "n", "abc" )
						.executeUpdate()
		);
	}

	@Test
	public void testJoinedSubclassRootUpdateRestricted(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "update Customer set name = :n where id = :d" )
						.setParameter( "n", "abc" )
						.setParameter( "d", 1 )
						.executeUpdate()
		);
	}
}
