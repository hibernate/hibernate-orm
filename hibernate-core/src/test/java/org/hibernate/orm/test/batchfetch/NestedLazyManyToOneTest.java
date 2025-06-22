/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.MultiKeyLoadHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(
		annotatedClasses = {
				NestedLazyManyToOneTest.AbstractEntity.class,
				NestedLazyManyToOneTest.Entity1.class,
				NestedLazyManyToOneTest.Entity2.class,
				NestedLazyManyToOneTest.Entity3.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "5"))
@JiraKey("HHH-16043")
public class NestedLazyManyToOneTest {
	private static final String QUESTION_MARK = "\\?";

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		final Entity1 entity1 = new Entity1();
		entity1.setId( "0" );

		final Set<Entity2> entities2 = new HashSet<>();
		for ( int i = 0; i < 8; i++ ) {
			final Entity2 entity2 = new Entity2();
			entity2.setId( entity1.getId() + "_" + i );
			entity2.setParent( entity1 );
			entities2.add( entity2 );

			// add nested children only to first and last entity
			if ( i == 0 || i == 7 ) {
				final Set<Entity3> entities3 = new HashSet<>();
				for ( int j = 0; j < 5; j++ ) {
					final Entity3 entity3 = new Entity3();
					entity3.setId( entity2.getId() + "_" + j );
					entity3.setParent( entity2 );
					entities3.add( entity3 );
				}
				entity2.setChildren( entities3 );
			}
		}
		entity1.setChildren( entities2 );

		scope.inTransaction( session -> {
			session.persist( entity1 );
		} );
	}

	@Test
	public void testGetFirstLevelChildren(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			Entity1 fromDb = session.find( Entity1.class, "0" );
			Set<Entity2> children = fromDb.getChildren();
			assertEquals( 8, children.size() );
			statementInspector.assertExecutedCount( 2 ); // 1 for Entity1, 1 for Entity2
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 1, QUESTION_MARK, 1 );
		} );
	}

	@Test
	public void testGetNestedChildrenLessThanBatchSize(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( session -> {
			Entity1 entity1 = session.find( Entity1.class, "0" );
			int i = 0;
			for ( Entity2 child2 : entity1.getChildren() ) {
				// get only first 5 (< batch size) elements
				// this doesn't trigger an additional query only because entity1.children
				// are ordered with @OrderBy, and we always get the first 5 first
				if ( i++ >= 5 ) {
					break;
				}
				else {
					Set<Entity3> children3 = child2.getChildren();
					if ( child2.getId().equals( "0_0" ) ) {
						assertEquals( 5, children3.size(), "Size of `Child2(0_0).children3` did not match expectation" );
					}
					else {
						assertEquals( 0, children3.size(), "Size of `Child2(" + child2.getId() + ").children3` did not match expectation" );
					}
				}
			}

			assertEquals( 8, entity1.getChildren().size() );
			// 1 for Entity1, 1 for Entity2, 1 for Entity3
			statementInspector.assertExecutedCount( 3 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 1, QUESTION_MARK, 1 );
			if ( MultiKeyLoadHelper.supportsSqlArrayType( scope.getSessionFactory().getJdbcServices().getDialect() ) ) {
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 2 ), '?' ) ).isEqualTo( 1 );
			}
			else {
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 2 ), '?' ) ).isEqualTo( 5 );
			}
		} );
	}

	@Test
	public void testGetNestedChildrenMoreThanBatchSize(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( session -> {
			Entity1 entity1 = session.find( Entity1.class, "0" );
			for ( Entity2 child2 : entity1.getChildren() ) {
				Set<Entity3> children3 = child2.getChildren();
				if ( child2.getId().equals( "0_0" ) || child2.getId().equals( "0_7" ) ) {
					assertEquals( 5, children3.size() );
				}
				else {
					assertEquals( 0, children3.size() );
				}
			}

			assertThat( entity1.getChildren() ).hasSize( 8 );
			// 1 for Entity1, 1 for Entity2, 2 for Entity3
			assertThat( statementInspector.getSqlQueries() ).hasSize( 4 );
			assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 1 ), '?' ) ).isEqualTo( 1 );
			if ( MultiKeyLoadHelper.supportsSqlArrayType( scope.getSessionFactory().getJdbcServices().getDialect() ) ) {
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 2 ), '?' ) ).isEqualTo( 1 );
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 3 ), '?' ) ).isEqualTo( 1 );
			}
			else {
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 2 ), '?' ) ).isEqualTo( 5 );
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 3 ), '?' ) ).isEqualTo( 5 );
			}
		} );
	}

	@MappedSuperclass
	public static class AbstractEntity {
		@Id
		private String id;

		private String name;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "#" + getId();
		}
	}


	@Entity(name = "Entity1")
	public static class Entity1 extends AbstractEntity {
		@OneToMany(mappedBy = "parent", cascade = ALL, orphanRemoval = true)
		@OrderBy("id")
		private Set<Entity2> children = new HashSet<>();

		public Set<Entity2> getChildren() {
			return children;
		}

		public void setChildren(Set<Entity2> children) {
			this.children = children;
		}
	}

	@Entity(name = "Entity2")
	public static class Entity2 extends AbstractEntity {
		@ManyToOne(fetch = LAZY)
		private Entity1 parent;

		@OneToMany(mappedBy = "parent", cascade = ALL, orphanRemoval = true)
		private Set<Entity3> children = new HashSet<>();

		public Entity1 getParent() {
			return parent;
		}

		public void setParent(Entity1 parent) {
			this.parent = parent;
		}

		public Set<Entity3> getChildren() {
			return children;
		}

		public void setChildren(Set<Entity3> children) {
			this.children = children;
		}
	}

	@Entity(name = "Entity3")
	public static class Entity3 extends AbstractEntity {
		@ManyToOne(fetch = LAZY)
		private Entity2 parent;

		public Entity2 getParent() {
			return parent;
		}

		public void setParent(Entity2 parent) {
			this.parent = parent;
		}
	}
}
