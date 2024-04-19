/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize.access;

import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.orm.test.boot.models.categorize.CategorizationTestsHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that AnnotationPlacementException is thrown when we find
 * persistence annotations on a member that is not backing a
 * persistence attribute.
 * <p/>
 * Note that this, and other "flexibilities", are tolerated in versions prior to 7.0
 *
 * @author Steve Ebersole
 */
public class AnnotationPlacementExceptionTests {
	@Test @SuppressWarnings("JUnitMalformedDeclaration")
	@ServiceRegistry
	void testExtraAnnotations(ServiceRegistryScope scope) {
		try {
			CategorizationTestsHelper.buildCategorizedDomainModel(
					scope,
					ExplicitPropertyAccessEntity.class
			);
			fail( "Expecting AnnotationPlacementException" );
		}
		catch (AnnotationPlacementException expected) {
		}

		try {
			CategorizationTestsHelper.buildCategorizedDomainModel(
					scope,
					ImplicitPropertyAccessEntity.class
			);
			fail( "Expecting AnnotationPlacementException" );
		}
		catch (AnnotationPlacementException expected) {
		}

		try {
			CategorizationTestsHelper.buildCategorizedDomainModel(
					scope,
					ExplicitFieldAccessEntity.class
			);
			fail( "Expecting AnnotationPlacementException" );
		}
		catch (AnnotationPlacementException expected) {
		}

		try {
			CategorizationTestsHelper.buildCategorizedDomainModel(
					scope,
					ImplicitFieldAccessEntity.class
			);
			fail( "Expecting AnnotationPlacementException" );
		}
		catch (AnnotationPlacementException expected) {
		}
	}

	@Entity(name="ExplicitPropertyAccessEntity")
	@Access(AccessType.PROPERTY)
	public static class ExplicitPropertyAccessEntity {
		private Integer id;
		@Column(name="wrong_place")
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="ImplicitPropertyAccessEntity")
	public static class ImplicitPropertyAccessEntity {
		private Integer id;
		@Column(name="wrong_place")
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="ExplicitFieldAccessEntity")
	@Access(AccessType.FIELD)
	public static class ExplicitFieldAccessEntity {
		@Id
		private Integer id;
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Column(name="wrong_place")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="ImplicitFieldAccessEntity")
	public static class ImplicitFieldAccessEntity {
		@Id
		private Integer id;
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Column(name="wrong_place")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
