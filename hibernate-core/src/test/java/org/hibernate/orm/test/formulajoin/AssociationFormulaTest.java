/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.formulajoin;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-9952")
@DomainModel(
		annotatedClasses = {
				AssociationFormulaTest.Parent.class,
				AssociationFormulaTest.Child.class
		}
)
@SessionFactory
public class AssociationFormulaTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		Child child = new Child( new EmbeddableId( "test", 2 ), "c1" );
		Parent parent = new Parent( new EmbeddableId( "test", 1 ), "p1", child );

		Parent parent2 = new Parent( new EmbeddableId( "null", 3 ), "p2" );

		scope.inTransaction(
				session -> {
					session.persist( parent );
					session.persist( parent2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent loaded = session.createQuery(
							"select e from Parent e inner join e.child o",
							Parent.class
					).uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getId().getId2() ).isEqualTo( 1 );
					assertThat( loaded.getChild().getId().getId2() ).isEqualTo( 2 );
					assertThat( Hibernate.isInitialized( loaded.getChild() ) ).isFalse();
					Hibernate.initialize( loaded.getChild() );
				}
		);
	}

	@Test
	public void testJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent loaded = session.createQuery(
							"select e from Parent e inner join fetch e.child o",
							Parent.class
					).uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getId().getId2() ).isEqualTo( 1 );
					assertThat( Hibernate.isInitialized( loaded.getChild() ) ).isTrue();
					assertThat( loaded.getChild().getId().getId2() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent loaded = session.createQuery( "from Parent e where e.child is null", Parent.class )
							.uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getId().getId2() ).isEqualTo( 3 );
					assertThat( loaded.getChild() ).isNull();
				}
		);

		scope.inTransaction(
				session -> {
					Parent loaded = session.createQuery( "from Parent e where e.child.id.id2 is null", Parent.class )
							.uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getId().getId2() ).isEqualTo( 3 );
					assertThat( loaded.getChild() ).isNull();
				}
		);

		scope.inTransaction(
				session -> {
					Child child = new Child( new EmbeddableId( "test", 2 ), "c2" );
					Parent loaded = session.createQuery( "from Parent e where e.child = :child", Parent.class )
							.setParameter( "child", child )
							.uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getId().getId2() ).isEqualTo( 1 );
					assertThat( loaded.getChild() ).isNotNull();
					assertThat( loaded.getChild().getId().getId2() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent loaded = session.createQuery( "from Parent e where e.id.id2 = 1", Parent.class )
							.uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getChild() ).isNotNull();
					Child child = new Child( new EmbeddableId( "test", 3 ), "c3" );
					loaded.setChild( child );
				}
		);

		scope.inTransaction(
				session -> {
					Parent loaded = session.createQuery( "from Parent e where e.id.id2 = 3", Parent.class )
							.uniqueResult();
					assertThat( loaded ).isNotNull();
					assertThat( loaded.getChild() ).isNull();
					Child child = new Child( new EmbeddableId( "test", 4 ), "c4" );
					loaded.setChild( child );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child( new EmbeddableId( "test", 2 ), "c2" );
					assertThat(
							session.createMutationQuery( "delete Parent e where e.child = :child" )
									.setParameter( "child", child )
									.executeUpdate()
					).isEqualTo( 1 );
					Parent loaded = session.createQuery( "from Parent e where e.id.id2 = 1", Parent.class )
							.uniqueResult();
					assertThat( loaded ).isNull();
				}
		);
	}

//	@Test
//	public void testUpdateHql(SessionFactoryScope scope) {
//		scope.inTransaction(
//				session -> {
//					Child child = new Child( new EmbeddableId( "null", 4 ), "c4" );
//					assertThat(
//							session.createQuery( "update Parent e set e.child = :child where e.id.id2 = 3" )
//									.setParameter( "child", child )
//									.executeUpdate()
//					).isEqualTo( 1 );
//					Parent loaded = session.createQuery( "from Parent e where e.id.id2 = 3", Parent.class )
//							.uniqueResult();
//					assertThat( loaded ).isNotNull();
//					assertThat( loaded.getChild().getId().getId2() ).isEqualTo( 4 );
//				}
//		);
//	}
//
//	@Test
//	public void testUpdateHqlNull(SessionFactoryScope scope) {
//		scope.inTransaction(
//				session -> {
//					assertThat(
//							session.createQuery( "update Parent e set e.child = null where e.id.id2 = 1" )
//									.executeUpdate()
//					).isEqualTo( 1 );
//					Parent loaded = session.createQuery( "from Parent e where e.id.id2 = 1", Parent.class )
//							.uniqueResult();
//					assertThat( loaded ).isNotNull();
//					assertThat( loaded.getChild().getId().getId2() ).isEqualTo( 4 );
//				}
//		);
//	}

	@Embeddable
	public static class EmbeddableId {
		private String id1;

		private int id2;

		public EmbeddableId() {
		}

		public EmbeddableId(String id1, int id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		public String getId1() {
			return id1;
		}

		public int getId2() {
			return id2;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {
		@EmbeddedId
		private EmbeddableId id;

		private String name;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "id1", value = "case when child is null then null else id1 end"))
		@JoinColumnOrFormula(column = @JoinColumn(referencedColumnName = "id2", name = "child"))
		private Child child;

		public Parent() {
		}

		public Parent(EmbeddableId id, String name) {
			this.id = id;
			this.name = name;
		}

		public Parent(EmbeddableId id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
		}

		public EmbeddableId getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child partial) {
			this.child = partial;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@EmbeddedId
		private EmbeddableId id;

		private String name;

		public Child() {
		}

		public Child(EmbeddableId id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public EmbeddableId getId() {
			return id;
		}
	}

}
