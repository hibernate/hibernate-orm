/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize.access;

import java.util.List;

import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.boot.models.categorize.spi.PersistentAttributeMemberResolver;
import org.hibernate.models.internal.ClassDetailsRegistryStandard;
import org.hibernate.models.internal.SourceModelBuildingContextImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * Mainly testing interpretation of an explicit `@Access(PROPERTY)` on a method
 *
 * @author Steve Ebersole
 */
public class ClassLevelFieldAccessTests {
	/**
	 *
	 * Tests implicit class-level FIELD access, based on `@Id` placement
	 */
	@Test
	void testImplicitFieldAccess() {
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				ImplicitFieldAccessEntity.class
		);

		final SourceModelBuildingContext modelBuildingContext = new SourceModelBuildingContextImpl( SIMPLE_CLASS_LOADING, jandexIndex );
		final ClassDetailsRegistry classDetailsRegistry = new ClassDetailsRegistryStandard( modelBuildingContext );
		final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( ImplicitFieldAccessEntity.class.getName() );
		final List<MemberDetails> attributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				classDetails,
				ClassAttributeAccessType.IMPLICIT_FIELD,
				null
		);
		assertThat( attributeMembers ).hasSize( 2 );
		assertThat( attributeMembers.stream().map( MemberDetails::getName ) ).contains( "id", "getName" );
	}

	/**
	 * Test behavior of an explicit `@Access(FIELD)` on the class
	 */
	@Test
	void testExplicitFieldAccess() {
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				ImplicitFieldAccessEntity.class
		);

		final SourceModelBuildingContext modelBuildingContext = new SourceModelBuildingContextImpl( SIMPLE_CLASS_LOADING, jandexIndex );
		final ClassDetailsRegistry classDetailsRegistry = new ClassDetailsRegistryStandard( modelBuildingContext );
		final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( ImplicitFieldAccessEntity.class.getName() );
		final List<MemberDetails> attributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				classDetails,
				ClassAttributeAccessType.EXPLICIT_FIELD,
				null
		);
		assertThat( attributeMembers ).hasSize( 2 );
		assertThat( attributeMembers.stream().map( MemberDetails::getName ) ).contains( "id", "getName" );
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

		@Access(AccessType.PROPERTY)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
