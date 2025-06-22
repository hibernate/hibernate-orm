/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.query.sqm.produce.function.FunctionArgumentException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedCollectionMaxIdTest.TestEntity.class,
		JoinedCollectionMaxIdTest.ListItem.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17104" )
public class JoinedCollectionMaxIdTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = new TestEntity();
			session.persist( entity );
			session.persist( new ListItem( 1L, "item_1", entity ) );
			session.persist( new ListItem( 2L, "item_2", entity ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ListItem" ).executeUpdate();
			session.createMutationQuery( "delete from TestEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testInvalidDynamicInstantiation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery( String.format(
						"select new %s(e, max(li)) from TestEntity e join e.listItems li group by e.id",
						DataProjection.class.getName()
				), Tuple.class ).getResultList();
				fail( "Calling a function with an entity-typed parameter should not be allowed" );
			}
			catch (Exception e) {
				assertThat( e.getCause() ).isInstanceOf( FunctionArgumentException.class );
				assertThat( e.getMessage() ).contains( "was not typed as an allowable function return type" );
			}
		} );
	}

	@Test
	public void testCorrectDynamicInstantiation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DataProjection result = session.createQuery( String.format(
					"select new %s(e, li) from TestEntity e join e.listItems li  " +
							"where li.id = (select max(li2.id) from e.listItems li2)",
					DataProjection.class.getName()
			), DataProjection.class ).getSingleResult();
			assertThat( result.getListItem().getName() ).isEqualTo( "item_2" );
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany( fetch = FetchType.LAZY, mappedBy = "entity" )
		private List<ListItem> listItems;

		public List<ListItem> getListItems() {
			return listItems;
		}
	}

	@Entity( name = "ListItem" )
	public static class ListItem {
		@Id
		private Long id;

		private String name;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "entity_id" )
		private TestEntity entity;

		public ListItem() {
		}

		public ListItem(Long id, String name, TestEntity entity) {
			this.id = id;
			this.name = name;
			this.entity = entity;
		}

		public String getName() {
			return name;
		}

		public TestEntity getEntity() {
			return entity;
		}
	}

	public static class DataProjection {
		private final TestEntity entity;

		private final ListItem listItem;

		public DataProjection(TestEntity entity, ListItem listItem) {
			this.entity = entity;
			this.listItem = listItem;
		}

		public TestEntity getEntity() {
			return entity;
		}

		public ListItem getListItem() {
			return listItem;
		}
	}
}
