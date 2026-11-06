package org.hibernate.orm.test.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
	annotatedClasses = {
		MapKeyEdgeCasesTest.Document.class,
		MapKeyEdgeCasesTest.Translation.class,
		MapKeyEdgeCasesTest.Locale.class,
		MapKeyEdgeCasesTest.SimpleEntity.class,
		MapKeyEdgeCasesTest.SimpleText.class
	}
)
@SessionFactory
public class MapKeyEdgeCasesTest {

	@Test
	public void testMultiColumnCompositeIdAsMapKey(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				Locale locale = new Locale();
				locale.setId(new LocaleId("en", "US"));
				session.persist(locale);

				Document doc = new Document();
				doc.setId(1L);
				session.persist(doc);

				Translation trans = new Translation();
				trans.setId(1L);
				trans.setDocument(doc);
				trans.setLocale(locale);
				trans.setText("Hello World");
				session.persist(trans);

				doc.getTranslations().put(locale, trans);
			}
		);

		scope.inTransaction(
			session -> {
				Document doc = session.find(Document.class, 1L);
				assert doc.getTranslations().size() == 1;
			}
		);
	}

	@Test
	public void testBasicIdAsMapKey(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				SimpleEntity entity = new SimpleEntity();
				entity.setId(1L);
				entity.setName("Simple");
				session.persist(entity);

				SimpleEntity parent = new SimpleEntity();
				parent.setId(2L);
				parent.setName("Parent");
				session.persist(parent);

				SimpleText text = new SimpleText();
				text.setId(1L);
				text.setParent(parent);
				text.setEntity(entity);
				text.setContent("Content");
				session.persist(text);

				parent.getTexts().put(entity, text);
			}
		);

		scope.inTransaction(
			session -> {
				SimpleEntity parent = session.find(SimpleEntity.class, 2L);
				assert parent.getTexts().size() == 1;
			}
		);
	}

	@Entity(name = "Document")
	public static class Document {
		@Id
		private Long id;

		@OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
		@MapKey(name = "locale")
		private Map<Locale, Translation> translations = new HashMap<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Map<Locale, Translation> getTranslations() {
			return translations;
		}

		public void setTranslations(Map<Locale, Translation> translations) {
			this.translations = translations;
		}
	}

	@Entity(name = "Translation")
	public static class Translation {
		@Id
		private Long id;

		@ManyToOne
		private Document document;

		@ManyToOne
		private Locale locale;

		private String text;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Document getDocument() {
			return document;
		}

		public void setDocument(Document document) {
			this.document = document;
		}

		public Locale getLocale() {
			return locale;
		}

		public void setLocale(Locale locale) {
			this.locale = locale;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = "Locale")
	public static class Locale {
		@EmbeddedId
		private LocaleId id;

		public LocaleId getId() {
			return id;
		}

		public void setId(LocaleId id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class LocaleId implements Serializable {
		private String language;
		private String country;

		public LocaleId() {
		}

		public LocaleId(String language, String country) {
			this.language = language;
			this.country = country;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LocaleId localeId = (LocaleId) o;
			if (language != null ? !language.equals(localeId.language) : localeId.language != null)
				return false;
			return country != null ? country.equals(localeId.country) : localeId.country == null;
		}

		@Override
		public int hashCode() {
			int result = language != null ? language.hashCode() : 0;
			result = 31 * result + (country != null ? country.hashCode() : 0);
			return result;
		}
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@MapKey(name = "entity")
		private Map<SimpleEntity, SimpleText> texts = new HashMap<>();

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

		public Map<SimpleEntity, SimpleText> getTexts() {
			return texts;
		}

		public void setTexts(Map<SimpleEntity, SimpleText> texts) {
			this.texts = texts;
		}
	}

	@Entity(name = "SimpleText")
	public static class SimpleText {
		@Id
		private Long id;

		@ManyToOne
		private SimpleEntity parent;

		@ManyToOne
		private SimpleEntity entity;

		private String content;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SimpleEntity getParent() {
			return parent;
		}

		public void setParent(SimpleEntity parent) {
			this.parent = parent;
		}

		public SimpleEntity getEntity() {
			return entity;
		}

		public void setEntity(SimpleEntity entity) {
			this.entity = entity;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}
}
