/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclassgeneratedvalue;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test.
 *
 * @author Stale W. Pedersen
 */
@DomainModel(
		annotatedClasses = {
				Simple.class,
				Simple2.class,
				Multiple.class
		}
)
@SessionFactory
public class IdClassGeneratedValueTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBaseLine(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple s1 = new Simple( 1L, 2L, 10 );
					session.persist( s1 );
					Simple s2 = new Simple( 2L, 3L, 20 );
					session.persist( s2 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Simple> simpleList = session.createQuery( "select s from Simple s" ).list();
					assertEquals( 2, simpleList.size() );
					Simple s1 = session.getReference( Simple.class, new SimplePK( 1L, 2L ) );
					assertEquals( s1.getQuantity(), 10 );
					session.clear();
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBaseLine2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple s1 = new Simple( 1L, 2L, 10 );
					session.persist( s1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Simple> simpleList = session.createQuery( "select s from Simple s" ).list();
					assertEquals( 1, simpleList.size() );
					Simple s1 = session.getReference( Simple.class, new SimplePK( 1L, 2L ) );
					assertEquals( s1.getQuantity(), 10 );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSingleGeneratedValue(SessionFactoryScope scope) {
		Long s1Id1 = scope.fromTransaction(
				session -> {
					Simple2 s1 = new Simple2( 200L, 10 );
					session.persist( s1 );
					Simple2 s2 = new Simple2( 300L, 20 );
					session.persist( s2 );
					return s1.getId1();
				}
		);

		scope.inTransaction(
				session -> {
					List<Simple2> simpleList = session.createQuery( "select s from Simple2 s" ).list();
					assertEquals( simpleList.size(), 2 );
					Simple2 s1 = session.getReference( Simple2.class, new SimplePK( s1Id1, 200L ) );
					assertEquals( s1.getQuantity(), 10 );
					session.clear();
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMultipleGeneratedValue(SessionFactoryScope scope) {
		List<Long> m1Ids = scope.fromTransaction(
				session -> {
					Multiple m1 = new Multiple( 1000L, 10 );
					session.persist( m1 );
					ArrayList<Long> mIds = new ArrayList<>();
					mIds.add( m1.getId1() );
					mIds.add( m1.getId2() );
					Multiple m2 = new Multiple( 2000L, 20 );
					session.persist( m2 );
					return mIds;
				}
		);

		scope.inTransaction(
				session -> {
					List<Multiple> simpleList = session.createQuery( "select m from Multiple m" ).list();
					assertEquals( simpleList.size(), 2 );
					Multiple m1 = session.getReference(
							Multiple.class,
							new MultiplePK( m1Ids.get( 0 ), m1Ids.get( 1 ), 1000L )
					);
					assertEquals( 10, m1.getQuantity() );
					session.clear();
				}
		);
	}
}
