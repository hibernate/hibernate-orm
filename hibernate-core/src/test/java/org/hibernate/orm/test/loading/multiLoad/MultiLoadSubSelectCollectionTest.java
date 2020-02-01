/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static javax.persistence.GenerationType.AUTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
@DomainModel(annotatedClasses = {
		MultiLoadSubSelectCollectionTest.Parent.class,
		MultiLoadSubSelectCollectionTest.Child.class
})
@ServiceRegistry
@SessionFactory
public class MultiLoadSubSelectCollectionTest {

	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.IGNORE );
			for ( int i = 1; i <= 60; i++ ) {
				final Parent p = new Parent( i, "Entity #" + i );
				for ( int j = 0; j < i; j++ ) {
					Child child = new Child();
					child.setParent( p );
					p.getChildren().add( child );
				}
				session.persist( p );
			}
		} );
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete Child" ).executeUpdate();
			session.createQuery( "delete Parent" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12740")
	public void testSubselect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Parent> list = session.byMultipleIds( Parent.class ).multiLoad( ids( 56 ) );
					assertEquals( 56, list.size() );

					// None of the collections should be loaded yet
					for ( Parent p : list ) {
						assertFalse( Hibernate.isInitialized( list.get( 0 ).children ) );
					}

					// When the first collection is loaded, the full batch of 50 collections
					// should be loaded.
					Hibernate.initialize( list.get( 0 ).children );

					for ( int i = 0; i < 50; i++ ) {
						assertTrue( Hibernate.isInitialized( list.get( i ).children ) );
						assertEquals( i + 1, list.get( i ).children.size() );
					}

					// The collections for the 51st through 56th entities should still be uninitialized
					for ( int i = 50; i < 56; i++ ) {
						assertFalse( Hibernate.isInitialized( list.get( i ).children ) );
					}

					// When the 51st collection gets initialized, the remaining collections should
					// also be initialized.
					Hibernate.initialize( list.get( 50 ).children );

					for ( int i = 50; i < 56; i++ ) {
						assertTrue( Hibernate.isInitialized( list.get( i ).children ) );
						assertEquals( i + 1, list.get( i ).children.size() );
					}
				}
		);
	}

	private Integer[] ids(int count) {
		Integer[] ids = new Integer[count];
		for ( int i = 1; i <= count; i++ ) {
			ids[i - 1] = i;
		}
		return ids;
	}

	@Entity(name = "Parent")
	@Table(name = "Parent")
	@BatchSize(size = 15)
	public static class Parent {
		Integer id;
		String text;
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(FetchMode.SUBSELECT)
		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = AUTO)
		private int id;

		@ManyToOne(fetch = FetchType.LAZY, optional = true)
		private Parent parent;

		public Child() {
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public int getId() {
			return id;
		}
	}
}
