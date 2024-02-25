/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		ManyToManyTreatJoinTest.ParentEntity.class,
		ManyToManyTreatJoinTest.SingleBase.class,
		ManyToManyTreatJoinTest.SingleSub1.class,
		ManyToManyTreatJoinTest.SingleSub2.class,
		ManyToManyTreatJoinTest.JoinedBase.class,
		ManyToManyTreatJoinTest.JoinedSub1.class,
		ManyToManyTreatJoinTest.JoinedSub2.class,
		ManyToManyTreatJoinTest.UnionBase.class,
		ManyToManyTreatJoinTest.UnionSub1.class,
		ManyToManyTreatJoinTest.UnionSub2.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class ManyToManyTreatJoinTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity parent1 = new ParentEntity( 1 );
			parent1.getSingleEntities().add( new SingleSub1( 2, 2 ) );
			parent1.getJoinedEntities().add( new JoinedSub1( 3, 3 ) );
			parent1.getUnionEntities().put( 4, new UnionSub1( 4, 4 ) );
			session.persist( parent1 );
			final ParentEntity parent2 = new ParentEntity( 5 );
			parent2.getSingleEntities().add( new SingleSub2( 6 ) );
			parent2.getJoinedEntities().add( new JoinedSub2( 7 ) );
			parent2.getUnionEntities().put( 8, new UnionSub2( 8 ) );
			session.persist( parent2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "from ParentEntity", ParentEntity.class )
				.getResultList()
				.forEach( session::remove ) );
	}

	@Test
	public void testSingleTableSelectChild(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select s.subProp from ParentEntity p join treat(p.singleEntities as SingleSub1) s",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 2 );
			inspector.assertNumberOfJoins( 0, 2 );
		} );
	}

	@Test
	public void testSingleTableSelectParent(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select p.id from ParentEntity p join treat(p.singleEntities as SingleSub1) s",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1 );
			// We always join the element table to restrict to the correct subtype
			inspector.assertNumberOfJoins( 0, 2 );
		} );
	}

	@Test
	public void testJoinedSelectChild(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select j.subProp from ParentEntity p join treat(p.joinedEntities as JoinedSub1) j",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 3 );
			inspector.assertNumberOfJoins( 0, 3 );
		} );
	}

	@Test
	public void testJoinedSelectParent(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select p.id from ParentEntity p join treat(p.joinedEntities as JoinedSub1) j",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1 );
			inspector.assertNumberOfJoins( 0, 3 );
		} );
	}

	@Test
	public void testTablePerClassSelectChild(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select u.subProp from ParentEntity p join treat(p.unionEntities as UnionSub1) u",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 4 );
			inspector.assertNumberOfJoins( 0, 2 );
		} );
	}

	@Test
	public void testTablePerClassSelectParent(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select p.id from ParentEntity p join treat(p.unionEntities as UnionSub1) u",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1 );
			inspector.assertNumberOfJoins( 0, 2 );
		} );
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		private Integer id;

		@ManyToMany( cascade = CascadeType.PERSIST )
		private List<SingleBase> singleEntities = new ArrayList<>();

		@ManyToMany( cascade = CascadeType.PERSIST )
		private Set<JoinedBase> joinedEntities = new HashSet<>();

		@ManyToMany( cascade = CascadeType.PERSIST )
		private Map<Integer, UnionBase> unionEntities = new HashMap<>();

		public ParentEntity() {
		}

		public ParentEntity(Integer id) {
			this.id = id;
		}

		public List<SingleBase> getSingleEntities() {
			return singleEntities;
		}

		public Set<JoinedBase> getJoinedEntities() {
			return joinedEntities;
		}

		public Map<Integer, UnionBase> getUnionEntities() {
			return unionEntities;
		}
	}

	@Entity( name = "SingleBase" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static abstract class SingleBase {
		@Id
		private Integer id;

		public SingleBase() {
		}

		public SingleBase(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "SingleSub1" )
	public static class SingleSub1 extends SingleBase {
		private Integer subProp;

		public SingleSub1() {
		}

		public SingleSub1(Integer id, Integer subProp) {
			super( id );
			this.subProp = subProp;
		}
	}

	@Entity( name = "SingleSub2" )
	public static class SingleSub2 extends SingleBase {
		public SingleSub2() {
		}

		public SingleSub2(Integer id) {
			super( id );
		}
	}

	@Entity( name = "JoinedBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class JoinedBase {
		@Id
		private Integer id;

		public JoinedBase() {
		}

		public JoinedBase(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "JoinedSub1" )
	public static class JoinedSub1 extends JoinedBase {
		private Integer subProp;

		public JoinedSub1() {
		}

		public JoinedSub1(Integer id, Integer subProp) {
			super( id );
			this.subProp = subProp;
		}
	}

	@Entity( name = "JoinedSub2" )
	public static class JoinedSub2 extends JoinedBase {
		public JoinedSub2() {
		}

		public JoinedSub2(Integer id) {
			super( id );
		}
	}

	@Entity( name = "UnionBase" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static abstract class UnionBase {
		@Id
		private Integer id;

		public UnionBase() {
		}

		public UnionBase(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "UnionSub1" )
	public static class UnionSub1 extends UnionBase {
		private Integer subProp;

		public UnionSub1() {
		}

		public UnionSub1(Integer id, Integer subProp) {
			super( id );
			this.subProp = subProp;
		}
	}

	@Entity( name = "UnionSub2" )
	public static class UnionSub2 extends UnionBase {
		public UnionSub2() {
		}

		public UnionSub2(Integer id) {
			super( id );
		}
	}
}
