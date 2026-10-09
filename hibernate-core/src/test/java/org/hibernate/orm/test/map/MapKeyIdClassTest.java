package org.hibernate.orm.test.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
	annotatedClasses = {
		MapKeyIdClassTest.Article.class,
		MapKeyIdClassTest.ArticleText.class,
		MapKeyIdClassTest.Language.class
	}
)
@SessionFactory
public class MapKeyIdClassTest {

	@Test
	public void testMapKeyWithIdClass(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				Language english = new Language();
				english.setCode("en");
				session.persist(english);

				Article article = new Article();
				article.setId(1L);
				session.persist(article);

				ArticleText text = new ArticleText();
				text.setId(1L);
				text.setArticle(article);
				text.setLanguage(english);
				text.setContent("English content");
				session.persist(text);

				article.getTexts().put(english, text);
			}
		);

		scope.inTransaction(
			session -> {
				Article article = session.find(Article.class, 1L);
				assert article.getTexts().size() == 1;
			}
		);
	}

	@Entity(name = "Article")
	public static class Article {
		@Id
		private Long id;

		@OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
		@MapKey(name = "language")
		private Map<Language, ArticleText> texts = new HashMap<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Map<Language, ArticleText> getTexts() {
			return texts;
		}

		public void setTexts(Map<Language, ArticleText> texts) {
			this.texts = texts;
		}
	}

	@Entity(name = "ArticleText")
	public static class ArticleText {
		@Id
		private Long id;

		@ManyToOne
		private Article article;

		@ManyToOne
		private Language language;

		private String content;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Article getArticle() {
			return article;
		}

		public void setArticle(Article article) {
			this.article = article;
		}

		public Language getLanguage() {
			return language;
		}

		public void setLanguage(Language language) {
			this.language = language;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	@Entity(name = "Language")
	@IdClass(LanguageId.class)
	public static class Language {
		@Id
		private String code;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	public static class LanguageId implements Serializable {
		private String code;

		public LanguageId() {
		}

		public LanguageId(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LanguageId that = (LanguageId) o;
			return code != null ? code.equals(that.code) : that.code == null;
		}

		@Override
		public int hashCode() {
			return code != null ? code.hashCode() : 0;
		}
	}
}
