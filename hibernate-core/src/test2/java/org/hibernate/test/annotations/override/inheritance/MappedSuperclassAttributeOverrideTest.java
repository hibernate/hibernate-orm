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
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MappedSuperclassAttributeOverrideTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
			CategoryEntity.class,
			TaxonEntity.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12609" )
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TaxonEntity taxon1 = new TaxonEntity();
			taxon1.setId( 1L );
			taxon1.setCode( "Taxon" );
			taxon1.setCatalogVersion( "C1" );

			entityManager.persist( taxon1 );

			TaxonEntity taxon2 = new TaxonEntity();
			taxon2.setId( 2L );
			taxon2.setCode( "Taxon" );
			taxon2.setCatalogVersion( "C2" );

			entityManager.persist( taxon2 );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			assertEquals(
				2,
				((Number) entityManager.createQuery(
					"select count(t) " +
					"from Taxon t " +
					"where t.code = :code" )
				.setParameter( "code", "Taxon" )
				.getSingleResult()).intValue()
			);
		} );
	}

	@MappedSuperclass
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

	@Entity(name = "Category")
	public static class CategoryEntity extends AbstractEntity {

	}

	@Entity(name = "Taxon")
	@Table(name = "taxon", uniqueConstraints = @UniqueConstraint(columnNames = { "catalog_version_id", "code" }))
	@AttributeOverride(name = "code", column = @Column(name = "code", nullable = false, unique = false))
	public static class TaxonEntity extends AbstractEntity {

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
