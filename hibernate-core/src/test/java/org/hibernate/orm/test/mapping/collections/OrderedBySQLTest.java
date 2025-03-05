/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.SQLOrder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class OrderedBySQLTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Article.class,
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG.name() );
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("Vlad Mihalcea");

			person.addArticle(
				new Article(
					"High-Performance JDBC",
					"Connection Management, Statement Caching, Batch Updates"
				)
			);
			person.addArticle(
				new Article(
					"High-Performance Hibernate",
					"Associations, Lazy fetching, Concurrency Control, Second-level Caching"
				)
			);
			entityManager.persist(person);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::collections-customizing-ordered-by-sql-clause-fetching-example[]
			Person person = entityManager.find(Person.class, 1L);
			assertEquals(
				"High-Performance Hibernate",
				person.getArticles().get(0).getName()
			);
			//end::collections-customizing-ordered-by-sql-clause-fetching-example[]
		});
	}

	//tag::collections-customizing-ordered-by-sql-clause-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany(
			mappedBy = "person",
			cascade = CascadeType.ALL
		)
		@SQLOrder("CHAR_LENGTH(name) DESC")
		private List<Article> articles = new ArrayList<>();

		//Getters and setters are omitted for brevity
	//end::collections-customizing-ordered-by-sql-clause-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Article> getArticles() {
			return articles;
		}

		public void addArticle(Article article) {
			article.setPerson(this);
			articles.add(article);
		}
	//tag::collections-customizing-ordered-by-sql-clause-mapping-example[]
	}

	@Entity(name = "Article")
	public static class Article {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private String content;

		@ManyToOne(fetch = FetchType.LAZY)
		private Person person;

		//Getters and setters are omitted for brevity
	//end::collections-customizing-ordered-by-sql-clause-mapping-example[]

		private Article() {
		}

		public Article(String name, String content) {
			this.name = name;
			this.content = content;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	//tag::collections-customizing-ordered-by-sql-clause-mapping-example[]
	}
	//end::collections-customizing-ordered-by-sql-clause-mapping-example[]
}
