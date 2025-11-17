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
import jakarta.persistence.criteria.JoinType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel( annotatedClasses = {
		TreatedLeftJoinInheritanceTest.ParentEntity.class,
		TreatedLeftJoinInheritanceTest.SingleTableEntity.class,
		TreatedLeftJoinInheritanceTest.SingleTableSubEntity.class,
		TreatedLeftJoinInheritanceTest.JoinedEntity.class,
		TreatedLeftJoinInheritanceTest.JoinedSubEntity.class,
		TreatedLeftJoinInheritanceTest.TablePerClassEntity.class,
		TreatedLeftJoinInheritanceTest.TablePerClassSubEntity.class,
		TreatedLeftJoinInheritanceTest.ChildEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16472" )
public class TreatedLeftJoinInheritanceTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var childEntity = new ChildEntity( 1L );
			session.persist( childEntity );
			var singleTableSubEntity = new SingleTableSubEntity( childEntity );
			session.persist( singleTableSubEntity );
			var joinedSubEntity = new JoinedSubEntity( childEntity );
			session.persist( joinedSubEntity );
			var tablePerClassSubEntity = new TablePerClassSubEntity( childEntity );
			session.persist( tablePerClassSubEntity );
			var parentEntityA = new ParentEntity( 1L, "entity1_a", null, null, null );
			session.persist( parentEntityA );
			var parentEntityB = new ParentEntity(
					2L,
					"entity1_b",
					singleTableSubEntity,
					joinedSubEntity,
					tablePerClassSubEntity
			);
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
			var join = root.join(
					session.getMetamodel()
							.entity( ParentEntity.class )
							.getSingularAttribute( "singleTableEntity", SingleTableEntity.class ),
					JoinType.LEFT
			);
			var treatedJoin = cb.treat(
					join,
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
			assertThat( resultList.get( 0 ).getSingleTableEntity() ).isNull();
			var subEntity = resultList.get( 1 ).getSingleTableEntity();
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
					session.getMetamodel()
							.entity( ParentEntity.class )
							.getSingularAttribute( "joinedEntity", JoinedEntity.class ),
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
			assertThat( resultList.get( 0 ).getJoinedEntity() ).isNull();
			var subEntity = resultList.get( 1 ).getJoinedEntity();
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
							.getSingularAttribute( "tablePerClassEntity", TablePerClassEntity.class ),
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
			assertThat( resultList.get( 0 ).getTablePerClassEntity() ).isNull();
			var subEntity = resultList.get( 1 ).getTablePerClassEntity();
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

		@ManyToOne
		private SingleTableEntity singleTableEntity;

		@ManyToOne
		private JoinedEntity joinedEntity;

		@ManyToOne
		private TablePerClassEntity tablePerClassEntity;

		public ParentEntity() {
		}

		public ParentEntity(
				Long id,
				String name,
				SingleTableEntity singleTableEntity,
				JoinedEntity joinedEntity,
				TablePerClassEntity tablePerClassEntity) {
			this.id = id;
			this.name = name;
			this.singleTableEntity = singleTableEntity;
			this.joinedEntity = joinedEntity;
			this.tablePerClassEntity = tablePerClassEntity;
		}

		public SingleTableEntity getSingleTableEntity() {
			return singleTableEntity;
		}

		public JoinedEntity getJoinedEntity() {
			return joinedEntity;
		}

		public TablePerClassEntity getTablePerClassEntity() {
			return tablePerClassEntity;
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
