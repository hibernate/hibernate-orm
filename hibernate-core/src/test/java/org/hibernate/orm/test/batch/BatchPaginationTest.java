/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				BatchPaginationTest.Article.class,
				BatchPaginationTest.Tag.class
		}
		,
		useCollectingStatementInspector = true
)
@JiraKey( value = "HHH-16005")
public class BatchPaginationTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Tag> tags = List.of( new Tag( "t1" ), new Tag( "t2" ), new Tag( "t3" ) );
					List<Tag> tags2 = List.of( new Tag( "t4" ), new Tag( "t5" ) );

					Article article = new Article( "0", tags );
					Article article2 = new Article();
					Article article3 = new Article( "3", tags2 );
					Article article4 = new Article();
					Article article5 = new Article();

					tags.forEach( entityManager::persist );
					tags2.forEach( entityManager::persist );

					entityManager.persist( article );
					entityManager.persist( article2 );
					entityManager.persist( article3 );
					entityManager.persist( article4 );
					entityManager.persist( article5 );
				}
		);
	}

	@Test
	void testIt(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();
					TypedQuery<Article> query = entityManager.createQuery( "select a from Article a", Article.class );
					List<Article> tech = query.setMaxResults( 20 ).getResultList();

					tech.stream()
							.map( ArticleResponseDto::new )
							.collect( Collectors.toList() );
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 2 );
				}
		);
	}

	public static class ArticleResponseDto {

		private final int id;
		private final List<TagDto> tags;

		public ArticleResponseDto(Article article) {
			this.id = article.getId();
			this.tags = article.getTags().stream()
					.map( TagDto::new )
					.collect( Collectors.toList());
		}
	}

	public static class TagDto {

		private final String name;

		public TagDto(Tag tag) {
			this.name = tag.getName();
		}
	}

	@Entity(name = "Tag")
	public static class Tag {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(unique = true)
		private String name;

		public Tag() {
		}

		public Tag(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Article")
	public static class Article {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private int id;

		private String categoryId;

		@BatchSize(size = 20)
		@ManyToMany
		private List<Tag> tags = new ArrayList<>();

		public Article() {
		}

		public Article(String categoryId, List<Tag> tags) {
			this.categoryId = categoryId;
			this.tags = tags;
		}

		public int getId() {
			return id;
		}

		public String getCategoryId() {
			return categoryId;
		}

		public List<Tag> getTags() {
			return tags;
		}
	}

}
