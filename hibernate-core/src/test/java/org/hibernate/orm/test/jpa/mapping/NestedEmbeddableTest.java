/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				NestedEmbeddableTest.Categorization.class,
				NestedEmbeddableTest.Category.class,
				NestedEmbeddableTest.CcmObject.class,
				NestedEmbeddableTest.Domain.class
		}
)
public class NestedEmbeddableTest {


	@Test
	public void test() {

	}

	@Entity
	@Table(name = "CATEGORIZATIONS")
	public static class Categorization implements Serializable {

		@Id
		@Column(name = "CATEGORIZATION_ID")
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long categorizationId;

		@ManyToOne
		@JoinColumn(name = "CATEGORY_ID")
		private Category category;

		@ManyToOne
		@JoinColumn(name = "OBJECT_ID")
		private CcmObject categorizedObject;

		public long getCategorizationId() {
			return categorizationId;
		}

		public void setCategorizationId(long categorizationId) {
			this.categorizationId = categorizationId;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public CcmObject getCategorizedObject() {
			return categorizedObject;
		}

		public void setCategorizedObject(CcmObject categorizedObject) {
			this.categorizedObject = categorizedObject;
		}
	}

	@Entity
	@Table(name = "CATEGORIES")
	public static class Category extends CcmObject implements Serializable {

		private static final long serialVersionUID = 1L;

		@Column(name = "NAME", nullable = false)
		private String name;

		@Embedded
		@AssociationOverride(
				name = "values",
				joinTable = @JoinTable(name = "CATEGORY_TITLES",
						joinColumns = {
								@JoinColumn(name = "OBJECT_ID")
						}
				))
		private LocalizedString title;

		@Embedded
		@AssociationOverride(
				name = "values",
				joinTable = @JoinTable(name = "CATEGORY_DESCRIPTIONS",
						joinColumns = {
								@JoinColumn(name = "OBJECT_ID")
						}
				))
		private LocalizedString description;

		@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
		@OrderBy("objectOrder ASC")
		private List<Categorization> objects;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public LocalizedString getTitle() {
			return title;
		}

		public void setTitle(LocalizedString title) {
			this.title = title;
		}

		public LocalizedString getDescription() {
			return description;
		}

		public void setDescription(LocalizedString description) {
			this.description = description;
		}
	}

	@Entity
	@Table(name = "CCM_OBJECTS")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class CcmObject implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		@Column(name = "OBJECT_ID")
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long objectId;

		@Column(name = "DISPLAY_NAME")
		private String displayName;

		@OneToMany(mappedBy = "categorizedObject", fetch = FetchType.LAZY)
		@OrderBy("categoryOrder ASC")
		private List<Categorization> categories;

		public long getObjectId() {
			return objectId;
		}

		public void setObjectId(long objectId) {
			this.objectId = objectId;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
	}

	@Entity
	@Table(name = "CATEGORY_DOMAINS")
	public static class Domain extends CcmObject {

		private static final long serialVersionUID = 1L;

		@Column(name = "DOMAIN_KEY", nullable = false, unique = true, length = 255)
		private String domainKey;

		@Embedded
		@AssociationOverride(
				name = "values",
				joinTable = @JoinTable(name = "DOMAIN_TITLES",
						joinColumns = {
								@JoinColumn(name = "OBJECT_ID")
						}))
		private LocalizedString title;

		@Embedded
		@AssociationOverride(
				name = "values",
				joinTable = @JoinTable(name = "DOMAIN_DESCRIPTIONS",
						joinColumns = {
								@JoinColumn(name = "OBJECT_ID")
						}))
		private LocalizedString description;

		public String getDomainKey() {
			return domainKey;
		}

		public void setDomainKey(String domainKey) {
			this.domainKey = domainKey;
		}

		public LocalizedString getTitle() {
			return title;
		}

		public void setTitle(LocalizedString title) {
			this.title = title;
		}

		public LocalizedString getDescription() {
			return description;
		}

		public void setDescription(LocalizedString description) {
			this.description = description;
		}
	}

	@Embeddable
	public static class LocalizedString implements Serializable {

		private static final long serialVersionUID = 1L;

		@ElementCollection(fetch = FetchType.EAGER)
		@MapKeyColumn(name = "LOCALE")
		@Column(name = "LOCALIZED_VALUE")
		@Lob
		// todo (6.0) : not sure of the "real" effect of dropping this as this test has not yet been re-enabled
		//@Type(type = "org.hibernate.type.TextType")
		private Map<Locale, String> values;

		public LocalizedString() {
			values = new HashMap<>();
		}

		public Map<Locale, String> getValues() {
			if ( values == null ) {
				return null;
			}
			else {
				return Collections.unmodifiableMap( values );
			}
		}

		protected void setValues(final Map<Locale, String> values) {
			if ( values == null ) {
				this.values = new HashMap<>();
			}
			else {
				this.values = new HashMap<>( values );
			}
		}

		public String getValue() {
			return getValue( Locale.getDefault() );
		}

		public String getValue(final Locale locale) {
			return values.get( locale );
		}

		public void addValue(final Locale locale, final String value) {
			values.put( locale, value );
		}

		public void removeValue(final Locale locale) {
			values.remove( locale );
		}

		public boolean hasValue(final Locale locale) {
			return values.containsKey( locale );
		}

		public Set<Locale> getAvailableLocales() {
			return values.keySet();
		}

	}
}
