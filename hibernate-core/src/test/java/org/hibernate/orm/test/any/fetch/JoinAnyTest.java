/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.fetch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {JoinAnyTest.AnyThing.class,
		JoinAnyTest.SomeThing.class, JoinAnyTest.SomeOtherThing.class})
class JoinAnyTest {

	@BeforeEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testJoin(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var someThing = new SomeThing();
			someThing.description = "Some thing";
			anyThing.anything = someThing;
			s.persist( someThing );
			s.persist( anyThing );
			var anyThingElse = new AnyThing();
			var otherThing = new SomeOtherThing();
			anyThingElse.anything = otherThing;
			s.persist( otherThing );
			s.persist( anyThingElse );
			var emptyThing = new AnyThing();
			s.persist( emptyThing );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( s -> {
			var results =
					s.createQuery( "select a, b from AnyThing a join a.anything b order by a.id",
									Object[].class )
						.getResultList();
			assertEquals( 3, results.size() );
			var result1 = results.get( 0 );
			var anyThing1 = (AnyThing) result1[0];
			assertInstanceOf( SomeThing.class, anyThing1.anything );
			assertInstanceOf( SomeThing.class, result1[1] );
			var someThing = (SomeThing) result1[1];
			assertEquals( "Some thing", someThing.getDescription() );
			assertSame( someThing, anyThing1.anything );
			var result2 = results.get( 1 );
			var anyThing2 = (AnyThing) result2[0];
			assertInstanceOf( SomeOtherThing.class, anyThing2.anything );
			assertInstanceOf( SomeOtherThing.class, result2[1] );
			var someOtherThing = (SomeOtherThing) result2[1];
			assertSame( someOtherThing, anyThing2.anything );
			var result3 = results.get( 2 );
			var anyThing3 = (AnyThing) result3[0];
			assertNull( result3[1] );
			assertNull( anyThing3.anything );
		} );
		assertEquals( 1, statementInspector.getSqlQueries().size() );
	}

	@Test
	void testJoinSelectAssociation(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var someThing = new SomeThing();
			someThing.description = "Some thing";
			anyThing.anything = someThing;
			s.persist( someThing );
			s.persist( anyThing );
			var anyThingElse = new AnyThing();
			var otherThing = new SomeOtherThing();
			anyThingElse.anything = otherThing;
			s.persist( otherThing );
			s.persist( anyThingElse );
			var emptyThing = new AnyThing();
			s.persist( emptyThing );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( s -> {
			var results = s.createQuery(
					"select b from AnyThing a join a.anything b order by a.id",
					Object.class
			).getResultList();
			assertEquals( 3, results.size() );
			assertInstanceOf( SomeThing.class, results.get( 0 ) );
			assertEquals( "Some thing", ( (SomeThing) results.get( 0 ) ).getDescription() );
			assertInstanceOf( SomeOtherThing.class, results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
		assertEquals( 1, statementInspector.getSqlQueries().size() );
	}

	@Test
	void testJoinTreat(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var someThing = new SomeThing();
			someThing.description = "Some thing";
			anyThing.anything = someThing;
			s.persist( someThing );
			s.persist( anyThing );
			var anyThingElse = new AnyThing();
			var otherThing = new SomeOtherThing();
			anyThingElse.anything = otherThing;
			s.persist( otherThing );
			s.persist( anyThingElse );
			var emptyThing = new AnyThing();
			s.persist( emptyThing );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( s -> {
			var results = s.createQuery(
					"select a, b from AnyThing a join treat(a.anything as SomeThing) b order by a.id",
					Object[].class
			).getResultList();
			assertEquals( 1, results.size() );
			var result = results.get( 0 );
			var anyThing = (AnyThing) result[0];
			assertInstanceOf( SomeThing.class, anyThing.anything );
			assertInstanceOf( SomeThing.class, result[1] );
			var someThing = (SomeThing) result[1];
			assertEquals( "Some thing", someThing.getDescription() );
			assertSame( someThing, anyThing.anything );
		} );
		assertEquals( 1, statementInspector.getSqlQueries().size() );
	}

	@Test
	void testJoinTreatSelectAssociation(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var someThing = new SomeThing();
			someThing.description = "Some thing";
			anyThing.anything = someThing;
			s.persist( someThing );
			s.persist( anyThing );
			var anyThingElse = new AnyThing();
			var otherThing = new SomeOtherThing();
			anyThingElse.anything = otherThing;
			s.persist( otherThing );
			s.persist( anyThingElse );
			var emptyThing = new AnyThing();
			s.persist( emptyThing );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( s -> {
			var results = s.createQuery(
					"select b from AnyThing a join treat(a.anything as SomeThing) b order by a.id",
					SomeThing.class
			).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( "Some thing", results.get( 0 ).getDescription() );
		} );
		assertEquals( 1, statementInspector.getSqlQueries().size() );
	}

	@Entity(name = "AnyThing")
	static class AnyThing {
		@Id
		@GeneratedValue
		Long id;

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "ANYTHING_ID")
		@Column(name = "ANYTHING_TYPE")
		@AnyDiscriminatorValue(
				discriminator = "SomeThing",
				entity = SomeThing.class)
		@AnyDiscriminatorValue(
				discriminator = "SomeOtherThing",
				entity = SomeOtherThing.class)
		Thing anything;
	}

	static class Thing {
	}

	@Entity(name = "SomeThing")
	static class SomeThing extends Thing {
		@Id
		@GeneratedValue
		Long id;

		String description;

		public String getDescription() {
			return description;
		}
	}

	@Entity(name = "SomeOtherThing")
	static class SomeOtherThing extends Thing {
		@Id
		@GeneratedValue
		Long id;
	}
}
