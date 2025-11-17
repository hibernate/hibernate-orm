/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-1268")
@Jpa(annotatedClasses = {
		UnidirectionalOneToManyUniqueConstraintOrderColumnTest.ParentData.class,
		UnidirectionalOneToManyUniqueConstraintOrderColumnTest.ChildData.class
})
public class UnidirectionalOneToManyUniqueConstraintOrderColumnTest {

	@BeforeAll
	protected void init(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ParentData parent = new ParentData();
					parent.id = 1L;
					entityManager.persist( parent );

					String[] childrenStr = new String[] {"One", "Two", "Three"};
					for ( String str : childrenStr ) {
						ChildData child = new ChildData( str );
						parent.getChildren().add( child );
					}
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-1268" )
	public void testRemovingAnElement(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );

					List<ChildData> children = parent.getChildren();
					children.remove( 0 );
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-1268" )
	public void testAddingAnElement(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );

					List<ChildData> children = parent.getChildren();
					children.add( 1, new ChildData( "Another" ) );
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-1268" )
	public void testRemovingAndAddingAnElement(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );

					List<ChildData> children = parent.getChildren();
					children.remove( 0 );
					children.add( 1, new ChildData( "Another" ) );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );

					List<String> childIds = parent.getChildren()
							.stream()
							.map( ChildData::toString )
							.collect( Collectors.toList() );

					int i = 0;

					assertEquals( "Two", childIds.get( i++ ));
					assertEquals( "Another", childIds.get( i++ ));
					assertEquals( "Three", childIds.get( i ));
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-1268" )
	public void testRemovingOneAndAddingTwoElements(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );

					List<ChildData> children = parent.getChildren();
					children.remove( 0 );
					children.add( 1, new ChildData( "Another" ) );
					children.add( new ChildData( "Another Another" ) );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );
					List<String> childIds = parent.getChildren()
							.stream()
							.map( ChildData::toString )
							.collect( Collectors.toList() );

					int i = 0;

					assertEquals( "Two", childIds.get( i++ ) );
					assertEquals( "Another", childIds.get( i++ ) );
					assertEquals( "Three", childIds.get( i++ ) );
					assertEquals( "Another Another", childIds.get( i ) );
				}
		);
	}

	@Entity(name = "ParentData")
	public static class ParentData {
		@Id
		long id;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumn(name = "parentId", nullable = false)
		@OrderColumn(name = "listOrder", nullable = false)
		private List<ChildData> children = new ArrayList<>();

		public List<ChildData> getChildren() {
			return children;
		}
	}

	@Entity(name = "ChildData")
	@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "parentId", "listOrder" }) })
	public static class ChildData {
		@Id
		@GeneratedValue
		long id;

		String childId;

		public ChildData() {
		}

		public ChildData(String id) {
			childId = id;
		}

		@Override
		public String toString() {
			return childId;
		}
	}

}
