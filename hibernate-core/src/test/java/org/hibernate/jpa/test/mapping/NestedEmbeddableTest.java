/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.persistence.AssociationOverride;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.criteria.components.Alias;
import org.hibernate.jpa.test.criteria.components.Client;
import org.hibernate.jpa.test.criteria.components.Client_;
import org.hibernate.jpa.test.criteria.components.Name_;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class NestedEmbeddableTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Categorization.class,
				Category.class,
				CcmObject.class,
				Domain.class
		};
	}

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
								@JoinColumn(name = "OBJECT_ID")}
				))
		private LocalizedString title;

		@Embedded
		@AssociationOverride(
				name = "values",
				joinTable = @JoinTable(name = "CATEGORY_DESCRIPTIONS",
						joinColumns = {
								@JoinColumn(name = "OBJECT_ID")}
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
								@JoinColumn(name = "OBJECT_ID")}))
		private LocalizedString title;

		@Embedded
		@AssociationOverride(
				name = "values",
				joinTable = @JoinTable(name = "DOMAIN_DESCRIPTIONS",
						joinColumns = {
								@JoinColumn(name = "OBJECT_ID")}))
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
		@Type(type = "org.hibernate.type.TextType")
		private Map<Locale, String> values;

		public LocalizedString() {
			values = new HashMap<>();
		}

		public Map<Locale, String> getValues() {
			if (values == null) {
				return null;
			} else {
				return Collections.unmodifiableMap( values);
			}
		}

		protected void setValues(final Map<Locale, String> values) {
			if (values == null) {
				this.values = new HashMap<>();
			} else {
				this.values = new HashMap<>(values);
			}
		}

		public String getValue() {
			return getValue(Locale.getDefault());
		}

		public String getValue(final Locale locale) {
			return values.get(locale);
		}

		public void addValue(final Locale locale, final String value) {
			values.put(locale, value);
		}

		public void removeValue(final Locale locale) {
			values.remove(locale);
		}

		public boolean hasValue(final Locale locale) {
			return values.containsKey(locale);
		}

		public Set<Locale> getAvailableLocales() {
			return values.keySet();
		}

	}
}
