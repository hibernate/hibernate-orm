/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-5694")
public class UnidirectionalOneToManyUniqueConstraintOrderColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testRemovingAnElement() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = new ParentData();
			entityManager.persist( parent );

			String[] childrenStr = new String[] {"One", "Two", "Three"};
			for ( String str : childrenStr ) {
				ChildData child = new ChildData( str );
				entityManager.persist( child );
				parent.getChildren().add( child );
			}

			entityManager.flush();

			List<ChildData> children = parent.getChildren();
			children.remove( 0 );
		} );
	}

	@Test
	public void testAddingAnElement() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = new ParentData();
			entityManager.persist( parent );

			String[] childrenStr = new String[] {"One", "Two", "Three"};
			for ( String str : childrenStr ) {
				ChildData child = new ChildData( str );
				entityManager.persist( child );
				parent.getChildren().add( child );
			}

			entityManager.flush();

			List<ChildData> children = parent.getChildren();
			children.add( 1, new ChildData( "Another" ) );
		} );
	}

	@Test
	public void testRemovingAndAddingAnElement() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = new ParentData();
			entityManager.persist( parent );

			String[] childrenStr = new String[] {"One", "Two", "Three"};
			for ( String str : childrenStr ) {
				ChildData child = new ChildData( str );
				entityManager.persist( child );
				parent.getChildren().add( child );
			}

			entityManager.flush();

			List<ChildData> children = parent.getChildren();
			children.remove( 0 );
			children.add( 1, new ChildData( "Another" ) );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = entityManager.find( ParentData.class, 1L );
			List<String> childIds = parent.getChildren().stream().map(ChildData::toString ).collect(Collectors.toList() );
			int i = 0;
			assertEquals( "Two", childIds.get( i++ ));
			assertEquals( "Another", childIds.get( i++ ));
			assertEquals( "Three", childIds.get( i++ ));
		} );
	}

	@Test
	public void testRemovingOneAndAddingTwoElements() {
		doInJPA( this::entityManagerFactory, entityManager -> {

					 ParentData parent = new ParentData();
					 entityManager.persist( parent );

					 String[] childrenStr = new String[] {"One", "Two", "Three"};
					 for ( String str : childrenStr ) {
						 ChildData child = new ChildData( str );
						 entityManager.persist( child );
						 parent.getChildren().add( child );
					 }

					 entityManager.flush();

					 List<ChildData> children = parent.getChildren();
					 children.remove( 0 );
					 children.add( 1, new ChildData( "Another" ) );
				   	 children.add( new ChildData( "Another Another" ) );
				 } );
		doInJPA( this::entityManagerFactory, entityManager -> {

					 ParentData parent = entityManager.find( ParentData.class, 1L );
					 List<String> childIds = parent.getChildren().stream().map(ChildData::toString ).collect(Collectors.toList() );
					 int i = 0;
					 assertEquals( "Two", childIds.get( i++ ));
					 assertEquals( "Another", childIds.get( i++ ));
					 assertEquals( "Three", childIds.get( i++ ));
					 assertEquals( "Another Another", childIds.get( i++ ));
				 } );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ParentData.class,
				ChildData.class
		};
	}

	@Entity(name = "ParentData")
	@Table(name = "PARENT")
	public static class ParentData {
		@Id
		@GeneratedValue
		long id;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumn(name = "parentId", nullable = false)
		@OrderColumn(name = "listOrder")
		private List<ChildData> children = new ArrayList<>();

		public List<ChildData> getChildren() {
			return children;
		}
	}

	@Entity(name = "ChildData")
	@Table(name = "CHILD", uniqueConstraints = {@UniqueConstraint(columnNames = {"parentId", "listOrder"})})
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
