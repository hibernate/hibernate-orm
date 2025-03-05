/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.sql.ast.SqlAstJoinType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Marco Belladelli
 */
public abstract class AbstractConcreteProxyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testSingleTable() {
		final SQLStatementInspector inspector = getStatementInspector();
		inspector.clear();
		// test find and association
		inSession( session -> {
			//tag::entity-concrete-proxy-find[]
			final SingleParent parent1 = session.find( SingleParent.class, 1L );
			assertThat( parent1.getSingle(), instanceOf( SingleSubChild1.class ) );
			assertThat( Hibernate.isInitialized( parent1.getSingle() ), is( false ) );
			final SingleSubChild1 proxy = (SingleSubChild1) parent1.getSingle();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			//end::entity-concrete-proxy-find[]
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
		inspector.clear();
		// test query and association
		inSession( session -> {
			final SingleParent parent2 = session.createQuery(
					"from SingleParent where id = 2",
					SingleParent.class
			).getSingleResult();
			assertThat( parent2.getSingle(), instanceOf( SingleChild2.class ) );
			assertThat( Hibernate.isInitialized( parent2.getSingle() ), is( false ) );
			final SingleChild2 proxy = (SingleChild2) parent2.getSingle();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
		inspector.clear();
		// test get reference
		inSession( session -> {
			//tag::entity-concrete-proxy-reference[]
			final SingleChild1 proxy1 = session.getReference( SingleChild1.class, 1L );
			assertThat( proxy1, instanceOf( SingleSubChild1.class ) );
			assertThat( Hibernate.isInitialized( proxy1 ), is( false ) );
			final SingleSubChild1 subChild1 = (SingleSubChild1) proxy1;
			assertThat( Hibernate.isInitialized( subChild1 ), is( false ) );
			//end::entity-concrete-proxy-reference[]
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 2 );
			inspector.clear();
			final SingleBase proxy2 = session.byId( SingleBase.class ).getReference( 2L );
			assertThat( proxy2, instanceOf( SingleChild2.class ) );
			assertThat( Hibernate.isInitialized( proxy2 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
	}

	@Test
	public void testJoined() {
		final SQLStatementInspector inspector = getStatementInspector();
		inspector.clear();
		// test find and association
		inSession( session -> {
			final JoinedParent parent1 = session.find( JoinedParent.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getJoined() ), is( false ) );
			assertThat( parent1.getJoined(), instanceOf( JoinedSubChild1.class ) );
			final JoinedSubChild1 proxy = (JoinedSubChild1) parent1.getJoined();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 4 );
		} );
		inspector.clear();
		// test query and association
		inSession( session -> {
			final JoinedParent parent2 = session.createQuery(
					"from JoinedParent where id = 2",
					JoinedParent.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getJoined() ), is( false ) );
			assertThat( parent2.getJoined(), instanceOf( JoinedChild2.class ) );
			final JoinedChild2 proxy = (JoinedChild2) parent2.getJoined();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 4 );
		} );
		inspector.clear();
		// test get reference
		inSession( session -> {
			final JoinedChild1 proxy1 = session.getReference( JoinedChild1.class, 1L );
			assertThat( proxy1, instanceOf( JoinedSubChild1.class ) );
			assertThat( Hibernate.isInitialized( proxy1 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.clear();
			final JoinedBase proxy2 = session.byId( JoinedBase.class ).getReference( 2L );
			assertThat( proxy2, instanceOf( JoinedChild2.class ) );
			assertThat( Hibernate.isInitialized( proxy2 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 3 );
		} );
	}

	@Test
	public void testJoinedDisc() {
		final SQLStatementInspector inspector = getStatementInspector();
		inspector.clear();
		// test find and association
		inSession( session -> {
			final JoinedDiscParent parent1 = session.find( JoinedDiscParent.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getJoinedDisc() ), is( false ) );
			assertThat( parent1.getJoinedDisc(), instanceOf( JoinedDiscSubChild1.class ) );
			final JoinedDiscSubChild1 proxy = (JoinedDiscSubChild1) parent1.getJoinedDisc();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
		inspector.clear();
		// test query and association
		inSession( session -> {
			final JoinedDiscParent parent2 = session.createQuery(
					"from JoinedDiscParent where id = 2",
					JoinedDiscParent.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getJoinedDisc() ), is( false ) );
			assertThat( parent2.getJoinedDisc(), instanceOf( JoinedDiscChild2.class ) );
			final JoinedDiscChild2 proxy = (JoinedDiscChild2) parent2.getJoinedDisc();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
		inspector.clear();
		// test get reference
		inSession( session -> {
			final JoinedDiscChild1 proxy1 = session.getReference( JoinedDiscChild1.class, 1L );
			assertThat( proxy1, instanceOf( JoinedDiscSubChild1.class ) );
			assertThat( Hibernate.isInitialized( proxy1 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 0 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			inspector.clear();
			final JoinedDiscBase proxy2 = session.byId( JoinedDiscBase.class ).getReference( 2L );
			assertThat( proxy2, instanceOf( JoinedDiscChild2.class ) );
			assertThat( Hibernate.isInitialized( proxy2 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, 0 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
	}

	@Test
	public void testUnion() {
		final SQLStatementInspector inspector = getStatementInspector();
		inspector.clear();
		// test find and association
		inSession( session -> {
			final UnionParent parent1 = session.find( UnionParent.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getUnion() ), is( false ) );
			assertThat( parent1.getUnion(), instanceOf( UnionSubChild1.class ) );
			final UnionSubChild1 proxy = (UnionSubChild1) parent1.getUnion();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "union", 3 );
		} );
		inspector.clear();
		// test query and association
		inSession( session -> {
			final UnionParent parent2 = session.createQuery(
					"from UnionParent where id = 2",
					UnionParent.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getUnion() ), is( false ) );
			assertThat( parent2.getUnion(), instanceOf( UnionChild2.class ) );
			final UnionChild2 proxy = (UnionChild2) parent2.getUnion();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "union", 3 );
		} );
		inspector.clear();
		// test get reference
		inSession( session -> {
			final UnionChild1 proxy1 = session.getReference( UnionChild1.class, 1L );
			assertThat( proxy1, instanceOf( UnionSubChild1.class ) );
			assertThat( Hibernate.isInitialized( proxy1 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "union", 1 );
			inspector.clear();
			final UnionBase proxy2 = session.byId( UnionBase.class ).getReference( 2L );
			assertThat( proxy2, instanceOf( UnionChild2.class ) );
			assertThat( Hibernate.isInitialized( proxy2 ), is( false ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "union", 3 );
		} );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			session.persist( new SingleParent( 1L, new SingleSubChild1( 1L, "1", "1" ) ) );
			session.persist( new JoinedParent( 1L, new JoinedSubChild1( 1L, "1", "1" ) ) );
			session.persist( new JoinedDiscParent( 1L, new JoinedDiscSubChild1( 1L, "1", "1" ) ) );
			session.persist( new UnionParent( 1L, new UnionSubChild1( 1L, "1", "1" ) ) );
			session.persist( new SingleParent( 2L, new SingleChild2( 2L, 2 ) ) );
			session.persist( new JoinedParent( 2L, new JoinedChild2( 2L, 2 ) ) );
			session.persist( new JoinedDiscParent( 2L, new JoinedDiscChild2( 2L, 2 ) ) );
			session.persist( new UnionParent( 2L, new UnionChild2( 2L, 2 ) ) );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			session.createMutationQuery( "delete from SingleParent" ).executeUpdate();
			session.createMutationQuery( "delete from SingleBase" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedParent" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedBase" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedDiscParent" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedDiscBase" ).executeUpdate();
			session.createMutationQuery( "delete from UnionParent" ).executeUpdate();
			session.createMutationQuery( "delete from UnionBase" ).executeUpdate();
		} );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClasses(
				SingleParent.class,
				SingleBase.class,
				SingleChild1.class,
				SingleSubChild1.class,
				SingleChild2.class,
				JoinedParent.class,
				JoinedBase.class,
				JoinedChild1.class,
				JoinedSubChild1.class,
				JoinedChild2.class,
				JoinedDiscParent.class,
				JoinedDiscBase.class,
				JoinedDiscChild1.class,
				JoinedDiscSubChild1.class,
				JoinedDiscChild2.class,
				UnionParent.class,
				UnionBase.class,
				UnionChild1.class,
				UnionSubChild1.class,
				UnionChild2.class
		);
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sfb.applyStatementInspector( new SQLStatementInspector() );
	}

	protected SQLStatementInspector getStatementInspector() {
		return (SQLStatementInspector) sessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	// InheritanceType.SINGLE_TABLE

	//tag::entity-concrete-proxy-mapping[]
	@Entity( name = "SingleParent" )
	public static class SingleParent {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private SingleBase single;

		public SingleParent() {
		}

		public SingleParent(Long id, SingleBase single) {
			this.id = id;
			this.single = single;
		}

		public SingleBase getSingle() {
			return single;
		}
	}

	@Entity( name = "SingleBase" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	@ConcreteProxy
	public static class SingleBase {
		@Id
		private Long id;

		public SingleBase() {
		}

		public SingleBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "SingleChild1" )
	public static class SingleChild1 extends SingleBase {
		private String child1Prop;

		public SingleChild1() {
		}

		public SingleChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "SingleSubChild1" )
	public static class SingleSubChild1 extends SingleChild1 {
		private String subChild1Prop;

		public SingleSubChild1() {
		}

		public SingleSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	// Other subtypes omitted for brevity
	//end::entity-concrete-proxy-mapping[]

	@Entity( name = "SingleChild2" )
	public static class SingleChild2 extends SingleBase {
		private Integer child2Prop;

		public SingleChild2() {
		}

		public SingleChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}

	// InheritanceType.JOINED

	@Entity( name = "JoinedParent" )
	public static class JoinedParent {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private JoinedBase joined;

		public JoinedParent() {
		}

		public JoinedParent(Long id, JoinedBase joined) {
			this.id = id;
			this.joined = joined;
		}

		public JoinedBase getJoined() {
			return joined;
		}
	}

	@Entity( name = "JoinedBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@ConcreteProxy
	public static class JoinedBase {
		@Id
		private Long id;

		public JoinedBase() {
		}

		public JoinedBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "JoinedChild1" )
	public static class JoinedChild1 extends JoinedBase {
		private String child1Prop;

		public JoinedChild1() {
		}

		public JoinedChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "JoinedSubChild1" )
	public static class JoinedSubChild1 extends JoinedChild1 {
		private String subChild1Prop;

		public JoinedSubChild1() {
		}

		public JoinedSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "JoinedChild2" )
	public static class JoinedChild2 extends JoinedBase {
		private Integer child2Prop;

		public JoinedChild2() {
		}

		public JoinedChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}

	// InheritanceType.JOINED + @DiscriminatorColumn

	@Entity( name = "JoinedDiscParent" )
	public static class JoinedDiscParent {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private JoinedDiscBase joinedDisc;

		public JoinedDiscParent() {
		}

		public JoinedDiscParent(Long id, JoinedDiscBase joinedDisc) {
			this.id = id;
			this.joinedDisc = joinedDisc;
		}

		public JoinedDiscBase getJoinedDisc() {
			return joinedDisc;
		}
	}

	@Entity( name = "JoinedDiscBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn( name = "disc_col" )
	@ConcreteProxy
	public static class JoinedDiscBase {
		@Id
		private Long id;

		public JoinedDiscBase() {
		}

		public JoinedDiscBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "JoinedDiscChild1" )
	public static class JoinedDiscChild1 extends JoinedDiscBase {
		private String child1Prop;

		public JoinedDiscChild1() {
		}

		public JoinedDiscChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "JoinedDiscSubChild1" )
	public static class JoinedDiscSubChild1 extends JoinedDiscChild1 {
		private String subChild1Prop;

		public JoinedDiscSubChild1() {
		}

		public JoinedDiscSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "JoinedDiscChild2" )
	public static class JoinedDiscChild2 extends JoinedDiscBase {
		private Integer child2Prop;

		public JoinedDiscChild2() {
		}

		public JoinedDiscChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}

	// InheritanceType.TABLE_PER_CLASS

	@Entity( name = "UnionParent" )
	public static class UnionParent {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private UnionBase union;

		public UnionParent() {
		}

		public UnionParent(Long id, UnionBase union) {
			this.id = id;
			this.union = union;
		}

		public UnionBase getUnion() {
			return union;
		}
	}

	@Entity( name = "UnionBase" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	@ConcreteProxy
	public static class UnionBase {
		@Id
		private Long id;

		public UnionBase() {
		}

		public UnionBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "UnionChild1" )
	public static class UnionChild1 extends UnionBase {
		private String child1Prop;

		public UnionChild1() {
		}

		public UnionChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "UnionSubChild1" )
	public static class UnionSubChild1 extends UnionChild1 {
		private String subChild1Prop;

		public UnionSubChild1() {
		}

		public UnionSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "UnionChild2" )
	public static class UnionChild2 extends UnionBase {
		private Integer child2Prop;

		public UnionChild2() {
		}

		public UnionChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}
}
