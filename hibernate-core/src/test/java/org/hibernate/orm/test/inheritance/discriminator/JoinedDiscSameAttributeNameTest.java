/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		JoinedDiscSameAttributeNameTest.Ancestor.class,
		JoinedDiscSameAttributeNameTest.DescendantA.class,
		JoinedDiscSameAttributeNameTest.DescendantB.class,
		JoinedDiscSameAttributeNameTest.DescendantTak.class,
		JoinedDiscSameAttributeNameTest.DescendantD.class,
})
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-19756" )
public class JoinedDiscSameAttributeNameTest {
	@Test
	void testCoalesceSameType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createTupleQuery();
			final var root = query.from( Ancestor.class );
			final var dscCRoot = cb.treat( root, DescendantTak.class );

			query.select( cb.tuple(
					root.get( "id" ).alias( "id" ),
					cb.coalesce(
							dscCRoot.get( "subtitle" ),
							dscCRoot.get( "title" )
					).alias( "description" )
			) ).orderBy( cb.asc( root.get( "id" ) ) );

			final var resultList = session.createSelectionQuery( query ).getResultList();
			assertResults( resultList, null, "title", null );
		} );
	}

	@Test
	void testCoalesceDifferentTypes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createTupleQuery();
			final var root = query.from( Ancestor.class );
			final var dscARoot = cb.treat( root, DescendantA.class );
			final var dscCRoot = cb.treat( root, DescendantTak.class );
			final var dscDRoot = cb.treat( root, DescendantD.class );

			query.select( cb.tuple(
					root.get( "id" ).alias( "id" ),
					cb.coalesce(
							dscDRoot.get( "subtitle" ),
							cb.coalesce(
									cb.coalesce(
											dscARoot.get( "subtitle" ),
											dscARoot.get( "title" )
									),
									cb.coalesce(
											dscCRoot.get( "subtitle" ),
											dscCRoot.get( "title" )
									)
							)
					).alias( "description" )
			) ).orderBy( cb.asc( root.get( "id" ) ) );

			final var resultList = session.createSelectionQuery( query ).getResultList();
			assertResults( resultList, null, "title", "subtitle" );
		} );
	}

	private static void assertResults(List<Tuple> resultList, String... expected) {
		assertThat( resultList ).hasSize( expected.length );
		for ( int i = 0; i < expected.length; i++ ) {
			final var r = resultList.get( i );
			assertThat( r.get( 0, Integer.class) ).isEqualTo( i + 1 );
			assertThat( r.get( 1, String.class ) ).isEqualTo( expected[i] );
		}
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var descendantA = new DescendantA();
			descendantA.id = 1;
			session.persist( descendantA );
			final var descendantTak = new DescendantTak();
			descendantTak.id = 2;
			descendantTak.title = "title";
			session.persist( descendantTak );
			final var descendantD = new DescendantD();
			descendantD.id = 3;
			descendantD.subtitle = "subtitle";
			session.persist( descendantD );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Ancestor")
	@Table(name = "t_ancestor")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "def_type_id")
	static abstract class Ancestor {
		@Id
		Integer id;
	}

	@Entity(name = "DescendantA")
	@DiscriminatorValue("A")
	@Table(name = "t_descendant_a")
	static class DescendantA extends Ancestor {
		String title;
		String subtitle;
	}

	@Entity(name = "DescendantB")
	@Table(name = "t_descendant_b")
	static abstract class DescendantB extends Ancestor {
	}

	@Entity(name = "DescendantTak")
	@DiscriminatorValue("C")
	@Table(name = "t_descendant_c")
	static class DescendantTak extends DescendantB {
		String title;
		String subtitle;
	}

	@Entity(name = "DescendantD")
	@DiscriminatorValue("D")
	@Table(name = "t_descendant_d")
	static class DescendantD extends DescendantB {
		String subtitle;
	}
}
