/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Almeras
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToOneInheritanceSubTypeTest.LinkedEntity.class,
		ManyToOneInheritanceSubTypeTest.SingleTableEntity.class,
		ManyToOneInheritanceSubTypeTest.SingleA.class,
		ManyToOneInheritanceSubTypeTest.SubSingleA.class,
		ManyToOneInheritanceSubTypeTest.SingleB.class,
		ManyToOneInheritanceSubTypeTest.JoinedEntity.class,
		ManyToOneInheritanceSubTypeTest.JoinedA.class,
		ManyToOneInheritanceSubTypeTest.SubJoinedA.class,
		ManyToOneInheritanceSubTypeTest.JoinedB.class,
		ManyToOneInheritanceSubTypeTest.UnionEntity.class,
		ManyToOneInheritanceSubTypeTest.UnionA.class,
		ManyToOneInheritanceSubTypeTest.SubUnionA.class,
		ManyToOneInheritanceSubTypeTest.UnionB.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16616" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17483" )
public class ManyToOneInheritanceSubTypeTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final LinkedEntity entity = new LinkedEntity( 1 );
			final SingleA singleA = new SingleA();
			session.persist( singleA );
			entity.getSingle().add( singleA );
			final SubSingleA subSingleA = new SubSingleA();
			session.persist( subSingleA );
			entity.getSingle().add( subSingleA );
			session.persist( new SingleB() );
			final JoinedA joinedA = new JoinedA();
			session.persist( joinedA );
			entity.getJoined().add( joinedA );
			final SubJoinedA subJoinedA = new SubJoinedA();
			session.persist( subJoinedA );
			entity.getJoined().add( subJoinedA );
			session.persist( new JoinedB() );
			final UnionA unionA = new UnionA();
			unionA.setLinkedEntity( entity );
			session.persist( unionA );
			final SubUnionA subUnionA = new SubUnionA();
			subUnionA.setLinkedEntity( entity );
			session.persist( subUnionA );
			session.persist( new UnionB() );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from SingleTableEntity" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedEntity" ).executeUpdate();
			session.createMutationQuery( "delete from UnionEntity" ).executeUpdate();
			session.createMutationQuery( "delete from LinkedEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final LinkedEntity entity = session.find( LinkedEntity.class, 1 );
			inspector.clear();
			assertThat( entity.getSingle() ).hasSize( 2 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 2 );
			assertThat( entity.getJoined() ).hasSize( 2 );
			assertThat( entity.getUnion() ).hasSize( 2 );
		} );
	}

	@Test
	public void testJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final LinkedEntity entity = session.createQuery(
					"from LinkedEntity e join fetch e.single",
					LinkedEntity.class
			).getSingleResult();
			assertThat( entity.getSingle() ).hasSize( 2 );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 2 );
		} );
		inspector.clear();
		scope.inTransaction( session -> {
			final LinkedEntity entity = session.createQuery(
					"from LinkedEntity e join fetch e.joined",
					LinkedEntity.class
			).getSingleResult();
			assertThat( entity.getJoined() ).hasSize( 2 );
			inspector.assertExecutedCount( 1 );
		} );
		inspector.clear();
		scope.inTransaction( session -> {
			final LinkedEntity entity = session.createQuery(
					"from LinkedEntity e join fetch e.union",
					LinkedEntity.class
			).getSingleResult();
			assertThat( entity.getUnion() ).hasSize( 2 );
			inspector.assertExecutedCount( 1 );
		} );
	}

	@Entity( name = "LinkedEntity" )
	public static class LinkedEntity {
		@Id
		private Integer id;

		@OneToMany
		@JoinColumn( name = "single_id" )
		private List<SingleA> single = new ArrayList<>();

		@OneToMany
		@JoinColumn( name = "joined_id" )
		private List<JoinedA> joined = new ArrayList<>();

		@OneToMany( mappedBy = "linkedEntity" )
		private List<UnionA> union = new ArrayList<>();

		public LinkedEntity() {
		}

		public LinkedEntity(Integer id) {
			this.id = id;
		}

		public List<SingleA> getSingle() {
			return single;
		}

		public List<JoinedA> getJoined() {
			return joined;
		}

		public List<UnionA> getUnion() {
			return union;
		}
	}

	// InheritanceType.SINGLE_TABLE

	@Entity( name = "SingleTableEntity" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	public static class SingleTableEntity {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity( name = "SingleA" )
	public static class SingleA extends SingleTableEntity {
	}

	@Entity( name = "SubSingleA" )
	public static class SubSingleA extends SingleA {
	}

	@Entity( name = "SingleB" )
	public static class SingleB extends SingleTableEntity {
	}

	// InheritanceType.JOINED

	@Entity( name = "JoinedEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedEntity {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity( name = "JoinedA" )
	public static class JoinedA extends JoinedEntity {
	}

	@Entity( name = "SubJoinedA" )
	public static class SubJoinedA extends JoinedA {
	}

	@Entity( name = "JoinedB" )
	public static class JoinedB extends JoinedEntity {
	}

	// InheritanceType.TABLE_PER_CLASS

	@Entity( name = "UnionEntity" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class UnionEntity {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity( name = "UnionA" )
	public static class UnionA extends UnionEntity {
		@ManyToOne
		@JoinColumn( name = "linked_id" )
		private LinkedEntity linkedEntity;

		public void setLinkedEntity(LinkedEntity linkedEntity) {
			this.linkedEntity = linkedEntity;
		}
	}

	@Entity( name = "SubUnionA" )
	public static class SubUnionA extends UnionA {
	}

	@Entity( name = "UnionB" )
	public static class UnionB extends UnionEntity {
	}
}
