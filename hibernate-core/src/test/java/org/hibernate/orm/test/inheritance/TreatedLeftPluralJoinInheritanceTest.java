/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel( annotatedClasses = {
		TreatedLeftPluralJoinInheritanceTest.ParentEntity.class,
		TreatedLeftPluralJoinInheritanceTest.SingleTableEntity.class,
		TreatedLeftPluralJoinInheritanceTest.SingleTableSubEntity.class,
		TreatedLeftPluralJoinInheritanceTest.JoinedEntity.class,
		TreatedLeftPluralJoinInheritanceTest.JoinedSubEntity.class,
		TreatedLeftPluralJoinInheritanceTest.TablePerClassEntity.class,
		TreatedLeftPluralJoinInheritanceTest.TablePerClassSubEntity.class,
		TreatedLeftPluralJoinInheritanceTest.ChildEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16472" )
public class TreatedLeftPluralJoinInheritanceTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var childEntity = new ChildEntity( 1L );
			session.persist( childEntity );
			var singleTableSubEntity = new SingleTableSubEntity( childEntity );
			var joinedSubEntity = new JoinedSubEntity( childEntity );
			var tablePerClassSubEntity = new TablePerClassSubEntity( childEntity );
			var parentEntityA = new ParentEntity( 1L, "entity1_a", null, null, null );
			session.persist( parentEntityA );
			var parentEntityB = new ParentEntity(
					2L,
					"entity1_b",
					List.of( singleTableSubEntity ),
					Set.of( joinedSubEntity ),
					List.of( tablePerClassSubEntity )
			);
			singleTableSubEntity.setParentEntity( parentEntityB );
			session.persist( singleTableSubEntity );
			joinedSubEntity.setParentEntity( parentEntityB );
			session.persist( joinedSubEntity );
			tablePerClassSubEntity.setParentEntity( parentEntityB );
			session.persist( tablePerClassSubEntity );
			session.persist( parentEntityB );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testSingleTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var childEntity = session.find( ChildEntity.class, 1L );
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( ParentEntity.class );
			var root = cq.from( ParentEntity.class );
			var listJoin = root.join(
					session.getMetamodel()
							.entity( ParentEntity.class )
							.getList( "singleTableEntities", SingleTableEntity.class ),
					JoinType.LEFT
			);
			var treatedJoin = cb.treat(
					listJoin,
					SingleTableSubEntity.class
			);
			var childPath = treatedJoin.get( "child" );
			cq.select( root ).where( cb.or(
					cb.equal( childPath, childEntity ),
					childPath.isNull()
			) ).orderBy( cb.asc( root.get( "id" ) ) );
			//noinspection removal
			var query = session.createQuery( cq );
			var resultList = query.getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ).getSingleTableEntities() ).isEmpty();
			assertThat( resultList.get( 1 ).getSingleTableEntities() ).hasSize( 1 );
			var subEntity = resultList.get( 1 ).getSingleTableEntities().get( 0 );
			assertThat( subEntity ).isInstanceOf( SingleTableSubEntity.class );
			assertThat( ( (SingleTableSubEntity) subEntity ).getChild().getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testJoined(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var childEntity = session.find( ChildEntity.class, 1L );
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( ParentEntity.class );
			var root = cq.from( ParentEntity.class );
			var join = root.join(
					session.getMetamodel().entity( ParentEntity.class ).getSet( "joinedEntities", JoinedEntity.class ),
					JoinType.LEFT
			);
			var treatedJoin = cb.treat(
					join,
					JoinedSubEntity.class
			);
			var childPath = treatedJoin.get( "child" );
			cq.select( root ).where( cb.or(
					cb.equal( childPath, childEntity ),
					childPath.isNull()
			) ).orderBy( cb.asc( root.get( "id" ) ) );
			//noinspection removal
			var query = session.createQuery( cq );
			var resultList = query.getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ).getJoinedEntities() ).isEmpty();
			assertThat( resultList.get( 1 ).getJoinedEntities() ).hasSize( 1 );
			var subEntity = resultList.get( 1 ).getJoinedEntities().iterator().next();
			assertThat( subEntity ).isInstanceOf( JoinedSubEntity.class );
			assertThat( ( (JoinedSubEntity) subEntity ).getChild().getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testTablePerClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var childEntity = session.find( ChildEntity.class, 1L );
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( ParentEntity.class );
			var root = cq.from( ParentEntity.class );
			var join = root.join(
					session.getMetamodel()
							.entity( ParentEntity.class )
							.getCollection( "tablePerClassEntities", TablePerClassEntity.class ),
					JoinType.LEFT
			);
			var treatedJoin = cb.treat(
					join,
					TablePerClassSubEntity.class
			);
			var childPath = treatedJoin.get( "child" );
			cq.select( root ).where( cb.or(
					cb.equal( childPath, childEntity ),
					childPath.isNull()
			) ).orderBy( cb.asc( root.get( "id" ) ) );
			//noinspection removal
			var query = session.createQuery( cq );
			var resultList = query.getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ).getTablePerClassEntities() ).isEmpty();
			assertThat( resultList.get( 1 ).getTablePerClassEntities() ).hasSize( 1 );
			var subEntity = resultList.get( 1 ).getTablePerClassEntities().iterator().next();
			assertThat( subEntity ).isInstanceOf( TablePerClassSubEntity.class );
			assertThat( ( (TablePerClassSubEntity) subEntity ).getChild().getId() ).isEqualTo( 1L );
		} );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		private Long id;

		private String name;

		@OneToMany( mappedBy = "parentEntity" )
		private List<SingleTableEntity> singleTableEntities;

		@OneToMany( mappedBy = "parentEntity" )
		private Set<JoinedEntity> joinedEntities;

		@OneToMany( mappedBy = "parentEntity" )
		private Collection<TablePerClassEntity> tablePerClassEntities;

		public ParentEntity() {
		}

		public ParentEntity(
				Long id,
				String name,
				List<SingleTableEntity> singleTableEntities,
				Set<JoinedEntity> joinedEntities,
				Collection<TablePerClassEntity> tablePerClassEntities) {
			this.id = id;
			this.name = name;
			this.singleTableEntities = singleTableEntities;
			this.joinedEntities = joinedEntities;
			this.tablePerClassEntities = tablePerClassEntities;
		}

		public List<SingleTableEntity> getSingleTableEntities() {
			return singleTableEntities;
		}

		public Set<JoinedEntity> getJoinedEntities() {
			return joinedEntities;
		}

		public Collection<TablePerClassEntity> getTablePerClassEntities() {
			return tablePerClassEntities;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "SingleTableEntity" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	public static class SingleTableEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private ParentEntity parentEntity;

		public ParentEntity getParentEntity() {
			return parentEntity;
		}

		public void setParentEntity(ParentEntity parentEntity) {
			this.parentEntity = parentEntity;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "SingleTableSubEntity" )
	public static class SingleTableSubEntity extends SingleTableEntity {
		@ManyToOne
		private ChildEntity child;

		public SingleTableSubEntity() {
		}

		public SingleTableSubEntity(ChildEntity child) {
			this.child = child;
		}

		public ChildEntity getChild() {
			return child;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private ParentEntity parentEntity;

		public ParentEntity getParentEntity() {
			return parentEntity;
		}

		public void setParentEntity(ParentEntity parentEntity) {
			this.parentEntity = parentEntity;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedSubEntity" )
	public static class JoinedSubEntity extends JoinedEntity {
		@ManyToOne
		private ChildEntity child;

		public JoinedSubEntity() {
		}

		public JoinedSubEntity(ChildEntity child) {
			this.child = child;
		}

		public ChildEntity getChild() {
			return child;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "TablePerClassEntity" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class TablePerClassEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private ParentEntity parentEntity;

		public ParentEntity getParentEntity() {
			return parentEntity;
		}

		public void setParentEntity(ParentEntity parentEntity) {
			this.parentEntity = parentEntity;
		}
	}

	@SuppressWarnings("unused")
	@Entity( name = "TablePerClassSubEntity" )
	public static class TablePerClassSubEntity extends TablePerClassEntity {
		@ManyToOne
		private ChildEntity child;

		public TablePerClassSubEntity() {
		}

		public TablePerClassSubEntity(ChildEntity child) {
			this.child = child;
		}

		public ChildEntity getChild() {
			return child;
		}
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity {
		@Id
		private Long id;

		public ChildEntity() {
		}

		public ChildEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}
}
