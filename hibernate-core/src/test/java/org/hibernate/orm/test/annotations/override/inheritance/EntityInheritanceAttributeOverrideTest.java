/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.override.inheritance;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.AnnotationException;
import org.hibernate.boot.model.internal.EntityBinder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKeyGroup;

import org.jboss.logging.Logger;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@JiraKeyGroup( value = {
		@JiraKey(value = "HHH-12609"),
		@JiraKey(value = "HHH-12654"),
		@JiraKey(value = "HHH-13172")
} )
@JiraKey(value = "HHH-12609, HHH-12654, HHH-13172")
public class EntityInheritanceAttributeOverrideTest extends EntityManagerFactoryBasedFunctionalTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, EntityBinder.class.getName() ) );

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				CategoryEntity.class,
				TaxonEntity.class,
				AbstractEntity.class
		};
	}

	@Test
	public void test() {
		try {
			produceEntityManagerFactory().close();
			fail();
		}
		catch (AnnotationException ae) {
			//expected
		}
	}

	@Entity(name = "AbstractEntity")
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

	@Entity(name = "Category")
	public static class CategoryEntity extends AbstractEntity {

	}

	@Entity(name = "Taxon")
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
