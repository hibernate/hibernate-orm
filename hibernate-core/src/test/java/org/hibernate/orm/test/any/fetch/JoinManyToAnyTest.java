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
import jakarta.persistence.JoinTable;
import org.hibernate.Hibernate;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {JoinManyToAnyTest.Parent.class,
		JoinManyToAnyTest.NormalChild.class, JoinManyToAnyTest.SpecialChild.class})
class JoinManyToAnyTest {
	@BeforeEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "Parent")
	static class Parent {
		@Id
		@GeneratedValue
		Long id;

		@ManyToAny(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@Column(name = "child_type")
		@AnyDiscriminatorValue(
				discriminator = "Normal",
				entity = NormalChild.class)
		@AnyDiscriminatorValue(
				discriminator = "Special",
				entity = SpecialChild.class)
		@JoinTable(name = "parent_child",
				joinColumns = @JoinColumn(name = "child_id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id"))
		Set<Child> children;
	}
	interface Child {}
	@Entity(name = "NormalChild")
	static class NormalChild implements Child {
		@Id
		@GeneratedValue
		Long id;
		int intValue;
	}
	@Entity(name = "SpecialChild")
	static class SpecialChild implements Child {
		@Id
		@GeneratedValue
		Long id;
		String strValue;
	}

	@Test
	void testJoin(SessionFactoryScope scope) {
		final var statementInspector = scope.getCollectingStatementInspector();
		prepareTestData( scope );
		statementInspector.clear();

		scope.inTransaction( s -> {
			final List<Object[]> results = s.createQuery(
					"select p, c from Parent p join p.children c",
					Object[].class
			).getResultList();
			assertEquals( 2, results.size() );

			final Parent parent = (Parent) results.get( 0 )[0];
			assertFalse( Hibernate.isInitialized( parent.children ) );
			assertEquals( 1, statementInspector.getSqlQueries().size() );

			assertSame( parent, results.get( 1 )[0] );

			final Object child1 = results.get( 0 )[1];
			final Object child2 = results.get( 1 )[1];
			assertTrue(
					( child1 instanceof NormalChild && child2 instanceof SpecialChild )
							|| ( child1 instanceof SpecialChild && child2 instanceof NormalChild )
			);

			assertEquals( 2, parent.children.size() );
			assertEquals( 2, statementInspector.getSqlQueries().size() );
			assertTrue( parent.children.contains( child1 ) );
			assertTrue( parent.children.contains( child2 ) );
		} );
	}

	@Test
	void testJoinSelectAssociation(SessionFactoryScope scope) {
		final var statementInspector = scope.getCollectingStatementInspector();
		prepareTestData( scope );
		statementInspector.clear();

		scope.inTransaction( s -> {
			final List<Object> results = s.createQuery(
					"select c from Parent p join p.children c",
					Object.class
			).getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1, statementInspector.getSqlQueries().size() );

			final NormalChild normalChild = results.stream()
					.filter( NormalChild.class::isInstance )
					.map( NormalChild.class::cast )
					.findFirst()
					.orElseThrow();
			final SpecialChild specialChild = results.stream()
					.filter( SpecialChild.class::isInstance )
					.map( SpecialChild.class::cast )
					.findFirst()
					.orElseThrow();

			assertInstanceOf( NormalChild.class, normalChild );
			assertEquals( 1, normalChild.intValue );
			assertInstanceOf( SpecialChild.class, specialChild );
			assertEquals( "special", specialChild.strValue );
		} );
	}

	@Test
	void testJoinTreat(SessionFactoryScope scope) {
		final var statementInspector = scope.getCollectingStatementInspector();
		prepareTestData( scope );
		statementInspector.clear();

		scope.inTransaction( s -> {
			final List<Object[]> results = s.createQuery(
					"select p, c from Parent p join treat(p.children as NormalChild) c",
					Object[].class
			).getResultList();
			assertEquals( 1, results.size() );

			final Parent parent = (Parent) results.get( 0 )[0];
			assertFalse( Hibernate.isInitialized( parent.children ) );
			assertEquals( 1, statementInspector.getSqlQueries().size() );

			assertInstanceOf( NormalChild.class, results.get( 0 )[1] );
			final NormalChild normalChild = (NormalChild) results.get( 0 )[1];
			assertEquals( 1, normalChild.intValue );

			assertEquals( 2, parent.children.size() );
			assertEquals( 2, statementInspector.getSqlQueries().size() );
			assertTrue( parent.children.contains( normalChild ) );
		} );
	}

	@Test
	void testJoinTreatSelectAssociation(SessionFactoryScope scope) {
		final var statementInspector = scope.getCollectingStatementInspector();
		prepareTestData( scope );
		statementInspector.clear();

		scope.inTransaction( s -> {
			final List<NormalChild> results = s.createQuery(
					"select c from Parent p join treat(p.children as NormalChild) c",
					NormalChild.class
			).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 1, results.get( 0 ).intValue );
		} );
		assertEquals( 1, statementInspector.getSqlQueries().size() );
	}

	private void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Parent parent = new Parent();
			final NormalChild normalChild = new NormalChild();
			normalChild.intValue = 1;
			final SpecialChild specialChild = new SpecialChild();
			specialChild.strValue = "special";
			parent.children = Set.of( normalChild, specialChild );
			s.persist( parent );
			s.persist( normalChild );
			s.persist( specialChild );
		} );
	}
}
