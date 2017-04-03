/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator;

import java.util.List;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Christian Beikov
 */
public class SingleTableRelationsTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {PostTable.class, Category.class, Post.class};
	}


	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT, "true" );
	}

	private void createTestData() {
		doInHibernate( this::sessionFactory, session -> {
			Category category7;
			session.persist( new Category( 1 ) );
			session.persist( new Category( 2 ) );
			session.persist( new Category( 3 ) );
			session.persist( new Category( 4 ) );
			session.persist( new Category( 5 ) );
			session.persist( new Category( 6 ) );
			session.persist( category7 = new Category( 7 ) );
			session.persist( new Post( 8, category7 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11375")
	public void testLazyInitialization() {
		createTestData();
		doInHibernate( this::sessionFactory, session -> {
			Category category7 = session.find( Category.class, 7 );
			// Must be empty because although Post and Category share the same column for their category relations,
			// the children must be based on entities that are of type Category
			Assert.assertTrue( category7.children.isEmpty() );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11375")
	public void testJoinFetch() {
		createTestData();
		doInHibernate( this::sessionFactory, session -> {
			Category category7 = session.createQuery(
					"SELECT c FROM " + Category.class.getName() + " c LEFT JOIN FETCH c.children WHERE c.id = :id",
					Category.class
			)
					.setParameter( "id", 7 )
					.getSingleResult();
			// Must be empty because although Post and Category share the same column for their category relations,
			// the children must be based on entities that are of type Category
			Assert.assertTrue( category7.children.isEmpty() );
		} );
	}

	@Entity
	@Table(name = "cp_post")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.INTEGER)
	public static class PostTable {

		@Id
		protected Integer id;

		public PostTable() {
		}

		public PostTable(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@DiscriminatorValue("1")
	public static class Category extends PostTable {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		protected Category category;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "category")
		protected List<Category> children;

		public Category() {
		}

		public Category(Integer id) {
			super( id );
		}

		public Category(Integer id, Category category) {
			super( id );
			this.category = category;
		}
	}

	@Entity
	@DiscriminatorValue("2")
	public static class Post extends PostTable {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		protected Category category;

		public Post() {
		}

		public Post(Integer id) {
			super( id );
		}

		public Post(Integer id, Category category) {
			super( id );
			this.category = category;
		}
	}

}
