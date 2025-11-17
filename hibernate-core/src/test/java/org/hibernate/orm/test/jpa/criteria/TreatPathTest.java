/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {
		TreatPathTest.Language.class,
		TreatPathTest.StringProperty.class,
		TreatPathTest.LocalTerm.class,
		TreatPathTest.Linkage.class
})
@JiraKey("HHH-16004")
@JiraKey("HHH-16014")
public class TreatPathTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Language language = new Language();
			language.setName( "Italian" );
			StringProperty stringProperty = new StringProperty( 1L, "ciao" );
			LocalTerm term = new LocalTerm();
			term.setLength( 4 );
			term.setLanguage( language );
			term.setAnyProperty( stringProperty );
			term.setSynonyms( new ArrayList<>() );
			term.setEmbeddableProperty( new EmbeddableType( "ciao" ) );
			Linkage linkage = new Linkage();
			linkage.setTerm( term );
			entityManager.persist( language );
			entityManager.persist( stringProperty );
			entityManager.persist( term );
			entityManager.persist( linkage );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Linkage" ).executeUpdate();
			entityManager.createQuery( "delete from LocalTerm" ).executeUpdate();
			entityManager.createQuery( "delete from StringProperty" ).executeUpdate();
			entityManager.createQuery( "delete from Language" ).executeUpdate();
		} );
	}

	@Test
	public void testTreatBasicValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> testCriteriaTreat( entityManager, "length", 4 ) );
	}

	@Test
	public void testTreatEntityValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Language language = entityManager.find( Language.class, 1L );
			testCriteriaTreat( entityManager, "language", language );
		} );
	}

	@Test
	public void testTreatPluralValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> testCriteriaTreat( entityManager, "synonyms", null, true ) );
	}

	@Test
	public void testTreatEmbeddableValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> testCriteriaTreat(
				entityManager,
				"embeddableProperty",
				new EmbeddableType( "ciao" )
		) );
	}

	@Test
	public void testTreatAnyValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StringProperty stringProperty = entityManager.find( StringProperty.class, 1L );
			testCriteriaTreat( entityManager, "anyProperty", stringProperty );
		} );
	}

	private void testCriteriaTreat(EntityManager entityManager, String property, Object value) {
		testCriteriaTreat( entityManager, property, value, false );
	}

	private void testCriteriaTreat(EntityManager entityManager, String property, Object value, boolean plural) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Linkage> criteria = cb.createQuery( Linkage.class );
		final Root<Linkage> root = criteria.from( Linkage.class );
		final Path<LocalTerm> asLocalTerm = cb.treat( root.get( "term" ), LocalTerm.class );
		final Predicate predicate;
		if ( plural ) {
			predicate = cb.isEmpty( asLocalTerm.get( property ) );
		}
		else {
			predicate = cb.equal( asLocalTerm.get( property ), value );
		}
		criteria.select( root ).where( predicate );
		List<Linkage> resultList = entityManager.createQuery( criteria ).getResultList();
		assertEquals( 1, resultList.size() );
	}

	@Entity(name = "Term")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class Term {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Language")
	public static class Language {
		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddableType {
		private String embeddedProperty;

		public EmbeddableType() {
		}

		public EmbeddableType(String embeddedProperty) {
			this.embeddedProperty = embeddedProperty;
		}

		public String getEmbeddedProperty() {
			return embeddedProperty;
		}

		public void setEmbeddedProperty(String embeddedProperty) {
			this.embeddedProperty = embeddedProperty;
		}
	}

	@Entity(name = "LocalTerm")
	public static class LocalTerm extends Term {
		private int length;

		@ManyToOne
		@JoinColumn(name = "language_id")
		private Language language;

		@ElementCollection
		private List<String> synonyms;

		private EmbeddableType embeddableProperty;

		@Any
		@AnyDiscriminator(DiscriminatorType.STRING)
		@AnyDiscriminatorValues({
				@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class),
		})
		@AnyKeyJavaClass(Long.class)
		@Column(name = "property_type")
		@JoinColumn(name = "property_id")
		private Property anyProperty;

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public Language getLanguage() {
			return language;
		}

		public void setLanguage(Language language) {
			this.language = language;
		}

		public List<String> getSynonyms() {
			return synonyms;
		}

		public void setSynonyms(List<String> synonyms) {
			this.synonyms = synonyms;
		}

		public EmbeddableType getEmbeddableProperty() {
			return embeddableProperty;
		}

		public void setEmbeddableProperty(EmbeddableType embeddableProperty) {
			this.embeddableProperty = embeddableProperty;
		}

		public Property getAnyProperty() {
			return anyProperty;
		}

		public void setAnyProperty(Property property) {
			this.anyProperty = property;
		}
	}

	@Entity(name = "Linkage")
	public static class Linkage {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "term_id", nullable = false)
		private Term term;

		public Long getId() {
			return id;
		}

		public Term getTerm() {
			return term;
		}

		public void setTerm(Term term) {
			this.term = term;
		}
	}

	public interface Property<T> {
		Long getId();

		T getValue();
	}


	@Entity(name = "StringProperty")
	public static class StringProperty implements Property<String> {
		@Id
		private Long id;

		@Column(name = "value_column")
		private String value;

		public StringProperty() {
		}

		public StringProperty(Long id, String value) {
			this.id = id;
			this.value = value;
		}

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String getValue() {
			return value;
		}
	}
}
