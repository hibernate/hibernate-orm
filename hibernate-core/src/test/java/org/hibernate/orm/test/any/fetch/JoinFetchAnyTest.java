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

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {JoinFetchAnyTest.AnyThing.class,
		JoinFetchAnyTest.SomeThing.class, JoinFetchAnyTest.SomeOtherThing.class})
class JoinFetchAnyTest {

	@BeforeEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test void testJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var someThing = new SomeThing();
			someThing.description = "Some thing";
			anyThing.anything = someThing;
			s.persist( someThing );
			s.persist( anyThing );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();

		// join fetch
		statementInspector.clear();
		scope.inTransaction( s -> {
			var singleResult =
					s.createQuery( "from AnyThing a join fetch a.anything", AnyThing.class )
							.getSingleResult();
			assertTrue( isInitialized( singleResult.anything ) );
			assertInstanceOf( SomeThing.class, singleResult.anything );
			assertEquals( "Some thing", ((SomeThing) singleResult.anything).description );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		} );
		statementInspector.clear();
		scope.inTransaction( s -> {
			var graph = s.createEntityGraph( AnyThing.class );
			graph.addAttributeNode( JoinFetchAnyTest_.AnyThing_.anything );
			var singleResult = s.find( graph, 1L );
			assertTrue( isInitialized( singleResult.anything ) );
			assertInstanceOf( SomeThing.class, singleResult.anything );
			assertEquals( "Some thing", ((SomeThing) singleResult.anything).description );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		} );

		// no join fetch
		statementInspector.clear();
		scope.inTransaction( s -> {
			var singleResult =
					s.createQuery( "from AnyThing a", AnyThing.class )
							.getSingleResult();
			assertFalse( isInitialized( singleResult.anything ) );
			assertInstanceOf( SomeThing.class, singleResult.anything );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
			assertEquals( "Some thing", ((SomeThing) singleResult.anything).getDescription() );
			assertEquals( 2, statementInspector.getSqlQueries().size() );
		} );
		statementInspector.clear();
		scope.inTransaction( s -> {
			var graph = s.createEntityGraph( AnyThing.class );
			var singleResult = s.find( graph, 1L );
			assertFalse( isInitialized( singleResult.anything ) );
			assertInstanceOf( SomeThing.class, singleResult.anything );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
			assertEquals( "Some thing", ((SomeThing) singleResult.anything).getDescription() );
			assertEquals( 2, statementInspector.getSqlQueries().size() );
		} );
	}

	@Test void testPolymorphicJoinFetch(SessionFactoryScope scope) {
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
		} );
		final var statementInspector = scope.getCollectingStatementInspector();

		// join fetch
		statementInspector.clear();
		scope.inTransaction( s -> {
			var results =
					s.createQuery( "from AnyThing a join fetch a.anything order by a.id", AnyThing.class )
							.getResultList();
			AnyThing anyThing = results.get( 0 );
			AnyThing anyThingElse = results.get( 1 );
			assertTrue( isInitialized( anyThing.anything ) );
			assertInstanceOf( SomeThing.class, anyThing.anything );
			assertTrue( isInitialized( anyThingElse.anything ) );
			assertInstanceOf( SomeOtherThing.class, anyThingElse.anything );
		} );
		statementInspector.clear();
		scope.inTransaction( s -> {
			var graph = s.createEntityGraph( AnyThing.class );
			graph.addAttributeNode( JoinFetchAnyTest_.AnyThing_.anything );
			var singleResult = s.find( graph, 1L );
			assertTrue( isInitialized( singleResult.anything ) );
			assertInstanceOf( SomeThing.class, singleResult.anything );
			assertEquals( "Some thing", ((SomeThing) singleResult.anything).description );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		} );
		statementInspector.clear();
		scope.inTransaction( s -> {
			var graph = s.createEntityGraph( AnyThing.class );
			graph.addAttributeNode( JoinFetchAnyTest_.AnyThing_.anything );
			var singleResult = s.find( graph, 2L );
			assertTrue( isInitialized( singleResult.anything ) );
			assertInstanceOf( SomeOtherThing.class, singleResult.anything );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		} );
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
