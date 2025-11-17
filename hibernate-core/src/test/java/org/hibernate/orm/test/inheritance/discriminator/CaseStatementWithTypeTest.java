/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		CaseStatementWithTypeTest.SingleParent.class,
		CaseStatementWithTypeTest.SingleChildA.class,
		CaseStatementWithTypeTest.SingleChildB.class,
		CaseStatementWithTypeTest.JoinedParent.class,
		CaseStatementWithTypeTest.JoinedChildA.class,
		CaseStatementWithTypeTest.JoinedChildB.class,
		CaseStatementWithTypeTest.JoinedDiscParent.class,
		CaseStatementWithTypeTest.JoinedDiscChildA.class,
		CaseStatementWithTypeTest.JoinedDiscChildB.class,
		CaseStatementWithTypeTest.UnionParent.class,
		CaseStatementWithTypeTest.UnionChildA.class,
		CaseStatementWithTypeTest.UnionChildB.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17413" )
public class CaseStatementWithTypeTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SingleChildA( 1L ) );
			session.persist( new SingleChildB( 2L ) );
			session.persist( new JoinedChildA( 1L ) );
			session.persist( new JoinedChildB( 2L ) );
			session.persist( new JoinedDiscChildA( 1L ) );
			session.persist( new JoinedDiscChildB( 2L ) );
			session.persist( new UnionChildA( 1L ) );
			session.persist( new UnionChildB( 2L ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testSingleTableInheritance(SessionFactoryScope scope) {
		executeQuery( scope, SingleParent.class, SingleChildA.class, SingleChildB.class, false );
		executeQuery( scope, SingleParent.class, SingleChildA.class, SingleChildB.class, true );
	}

	@Test
	public void testJoinedInheritance(SessionFactoryScope scope) {
		executeQuery( scope, JoinedParent.class, JoinedChildA.class, JoinedChildB.class, false );
		executeQuery( scope, JoinedParent.class, JoinedChildA.class, JoinedChildB.class, true );
	}

	@Test
	public void testJoinedInheritanceAndDiscriminator(SessionFactoryScope scope) {
		executeQuery( scope, JoinedDiscParent.class, JoinedDiscChildA.class, JoinedDiscChildB.class, false );
		executeQuery( scope, JoinedDiscParent.class, JoinedDiscChildA.class, JoinedDiscChildB.class, true );
	}

	@Test
	public void testTablePerClassInheritance(SessionFactoryScope scope) {
		executeQuery( scope, UnionParent.class, UnionChildA.class, UnionChildB.class, false );
		executeQuery( scope, UnionParent.class, UnionChildA.class, UnionChildB.class, true );
	}

	private <T> void executeQuery(
			SessionFactoryScope scope,
			Class<T> parent,
			Class<? extends T> childA,
			Class<? extends T> childB,
			boolean orderBy) {
		scope.inTransaction( session -> {
			final StringBuilder sb = new StringBuilder( "select p.id" );
			final String caseExpression = String.format(
					"case type(p) when %s then 'A' when %s then 'B' else null end",
					childA.getSimpleName(),
					childB.getSimpleName()
			);
			if ( orderBy ) {
				sb.append( String.format( " from %s p order by %s", parent.getSimpleName(), caseExpression ) );
			}
			else {
				sb.append( String.format( ", %s from %s p", caseExpression, parent.getSimpleName() ) );
			}
			//noinspection removal
			final List<Tuple> resultList = session.createQuery( sb.toString(), Tuple.class ).getResultList();
			assertThat( resultList ).hasSize( 2 );
			if ( orderBy ) {
				assertThat( resultList.stream().map( t -> t.get( 0, Long.class ) ) ).containsExactly( 1L, 2L );
			}
			else {
				assertThat( resultList.stream().map( t -> t.get( 1, String.class ) ) ).contains( "A", "B" );
			}
		} );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "SingleParent" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class SingleParent {
		@Id
		private Long id;

		public SingleParent() {
		}

		public SingleParent(Long id) {
			this.id = id;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "SingleChildA" )
	public static class SingleChildA extends SingleParent {
		public SingleChildA() {
		}

		public SingleChildA(Long id) {
			super( id );
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "SingleChildB" )
	public static class SingleChildB extends SingleParent {
		public SingleChildB() {
		}

		public SingleChildB(Long id) {
			super( id );
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "JoinedParent" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedParent {
		@Id
		private Long id;

		public JoinedParent() {
		}

		public JoinedParent(Long id) {
			this.id = id;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedChildA" )
	public static class JoinedChildA extends JoinedParent {
		public JoinedChildA() {
		}

		public JoinedChildA(Long id) {
			super( id );
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedChildB" )
	public static class JoinedChildB extends JoinedParent {
		public JoinedChildB() {
		}

		public JoinedChildB(Long id) {
			super( id );
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "JoinedDiscParent" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn
	public static class JoinedDiscParent {
		@Id
		private Long id;

		public JoinedDiscParent() {
		}

		public JoinedDiscParent(Long id) {
			this.id = id;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedDiscChildA" )
	public static class JoinedDiscChildA extends JoinedDiscParent {
		public JoinedDiscChildA() {
		}

		public JoinedDiscChildA(Long id) {
			super( id );
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedDiscChildB" )
	public static class JoinedDiscChildB extends JoinedDiscParent {
		public JoinedDiscChildB() {
		}

		public JoinedDiscChildB(Long id) {
			super( id );
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "UnionParent" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class UnionParent {
		@Id
		private Long id;

		public UnionParent() {
		}

		public UnionParent(Long id) {
			this.id = id;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "UnionChildA" )
	public static class UnionChildA extends UnionParent {
		public UnionChildA() {
		}

		public UnionChildA(Long id) {
			super( id );
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "UnionChildB" )
	public static class UnionChildB extends UnionParent {
		public UnionChildB() {
		}

		public UnionChildB(Long id) {
			super( id );
		}
	}
}
