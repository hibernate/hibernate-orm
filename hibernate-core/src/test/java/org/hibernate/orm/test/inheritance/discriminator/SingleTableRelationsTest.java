/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.util.List;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				SingleTableRelationsTest.PostTable.class,
				SingleTableRelationsTest.Category.class,
				SingleTableRelationsTest.Post.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT, value = "true"))
public class SingleTableRelationsTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
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

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-11375")
	public void testLazyInitialization(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Category category7 = session.find( Category.class, 7 );
					// Must be empty because although Post and Category share the same column for their category relations,
					// the children must be based on entities that are of type Category
					assertTrue( category7.children.isEmpty() );
				} );
	}

	@Test
	@JiraKey(value = "HHH-11375")
	public void testJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Category category7 = session.createQuery(
							"SELECT c FROM " + Category.class.getName() + " c LEFT JOIN FETCH c.children WHERE c.id = :id",
							Category.class
					)
							.setParameter( "id", 7 )
							.getSingleResult();
					// Must be empty because although Post and Category share the same column for their category relations,
					// the children must be based on entities that are of type Category
					assertTrue( category7.children.isEmpty() );
				} );
	}

	@Entity(name = "PostTable")
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

	@Entity(name = "Category")
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

	@Entity(name = "Post")
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
