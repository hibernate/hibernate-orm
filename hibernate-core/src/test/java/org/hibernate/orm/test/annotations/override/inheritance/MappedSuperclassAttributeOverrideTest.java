/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.override.inheritance;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				MappedSuperclassAttributeOverrideTest.CategoryEntity.class,
				MappedSuperclassAttributeOverrideTest.TaxonEntity.class
		}
)
public class MappedSuperclassAttributeOverrideTest {

	@Test
	@JiraKey(value = "HHH-12609")
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

		scope.inTransaction( entityManager -> {
			assertEquals(
					2,
					( (Number) entityManager.createQuery(
							"select count(t) " +
									"from Taxon t " +
									"where t.code = :code" )
							.setParameter( "code", "Taxon" )
							.getSingleResult() ).intValue()
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
