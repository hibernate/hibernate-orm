/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.override.inheritance;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Petar Tahchiev
 */
@TestForIssue( jiraKey = "HHH-12609, HHH-12654,HHH-13172" )
public class EntityInheritanceWithTablePerClassAttributeOverrideTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				CategoryEntity.class,
				TaxonEntity.class,
				AbstractEntity.class
		};
	}

	@Override
	public void buildEntityManagerFactory() {
		try {
			super.buildEntityManagerFactory();
		}
		catch (AnnotationException e) {
			e.printStackTrace();
			fail("Should not throw AnnotationException - TABLE_PER_CLASS should allow overriding attributes");
		}
	}

	@Test
	public void test() {
	}

	@Entity(name = "AbstractEntity1")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class AbstractEntity {

		@Id
		private Long id;

		@Column(name = "code", nullable = false, unique = true)
		private String code;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

	}

	@Entity(name = "Category1")
	public static class CategoryEntity extends AbstractEntity {

	}

	@Entity(name = "Taxon1")
	@Table(
			name = "taxon",
			uniqueConstraints = @UniqueConstraint(name = "category_code", columnNames = { "catalog_version_id", "code" })
	)
	@AttributeOverride(name = "code", column = @Column(name = "code", nullable = false, unique = false))
	public static class TaxonEntity extends CategoryEntity {

		@Column(name = "catalog_version_id")
		private String catalogVersion;

		public String getCatalogVersion() {
			return catalogVersion;
		}

		public void setCatalogVersion(String catalogVersion) {
			this.catalogVersion = catalogVersion;
		}
	}
}
