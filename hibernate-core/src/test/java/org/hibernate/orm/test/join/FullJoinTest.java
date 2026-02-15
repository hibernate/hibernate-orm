/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses =
		{FullJoinTest.Thing.class,
		FullJoinTest.OtherThing.class})
class FullJoinTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		prepareData( scope );
		scope.inTransaction( session -> {
			var result1 =
					session.createQuery( "select t, o from Thing t full join t.otherThing o", Object[].class )
							.getResultList();
			assertEquals( 3, result1.size() );
			var result2 =
					session.createQuery( "select t, o from OtherThing o full join o.things t", Object[].class )
							.getResultList();
			assertEquals( 3, result2.size() );
		} );
	}

	@Test
	void testNullsLastOrderBy(EntityManagerFactoryScope scope) {
		prepareData( scope );
		scope.inTransaction( session -> {
			final var result =
					session.createQuery(
							"select t.id, o.id from Thing t full join t.otherThing o order by t.id nulls last, o.id",
							Object[].class
					).getResultList();
			assertEquals( 3, result.size() );
			assertNotNull( result.get( 0 )[0] );
			assertNotNull( result.get( 1 )[0] );
			assertTrue(
					( (Number) result.get( 0 )[0] ).longValue() < ( (Number) result.get( 1 )[0] ).longValue()
			);
			assertNull( result.get( 2 )[0] );
			assertNotNull( result.get( 2 )[1] );
		} );
	}

	@Test
	void testSybaseNullPrecedenceAliasCounting(EntityManagerFactoryScope scope) {
		prepareData( scope );
		scope.inTransaction( session -> {
			final var result =
					session.createQuery(
							"select t.id from Thing t full join t.otherThing o order by t.id nulls last",
							Long.class
					).getResultList();
			assertEquals( 3, result.size() );
			assertNull( result.get( 2 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "Sybase does not allow union within subquery")
	void testNestedFullJoinInSubquery(EntityManagerFactoryScope scope) {
		prepareData( scope );
		scope.inTransaction( session -> {
			final var result =
					session.createQuery(
							"""
									select t.id from Thing t
										full join t.otherThing o
										where exists (select 1 from Thing t2 full join t2.otherThing o2 where o2.id is null)
									""",
							Long.class
					).getResultList();
			assertEquals( 3, result.size() );
		} );
	}

	@Test
	void testNestedScalarSubqueryWithOuterFullJoinOrderBy(EntityManagerFactoryScope scope) {
		prepareData( scope );
		scope.inTransaction( session -> {
			final var result =
					session.createQuery(
							"""
									select t.id from Thing t full join t.otherThing o
										where t.id = (select max(t2.id) from Thing t2)
										order by o.id
									""",
							Long.class
					).getResultList();
			assertEquals( 1, result.size() );
			assertNotNull( result.get( 0 ) );
		} );
	}

	private void prepareData(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "delete from OtherThing_Thing" ).executeUpdate();
			session.createQuery( "delete from Thing" ).executeUpdate();
			session.createQuery( "delete from OtherThing" ).executeUpdate();

			Thing thing = new Thing();
			OtherThing otherThing = new OtherThing();
			thing.otherThing = otherThing;
			otherThing.things.add( thing );
			session.persist( thing );
			session.persist( otherThing );
			session.persist( new Thing() );
			session.persist( new OtherThing() );
		} );
	}

	@Entity(name = "Thing")
	static class Thing {
		@Id @GeneratedValue
		Long id;
		String name;
		@ManyToOne
		OtherThing otherThing;
	}
	@Entity(name = "OtherThing")
	static class OtherThing {
		@Id @GeneratedValue
		Long id;
		@OneToMany
		Set<Thing> things = new HashSet<>();
	}
}
