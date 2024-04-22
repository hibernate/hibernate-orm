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
 * Mainly testing interpretation of an explicit `@Access(FIELD)` on a FIELD
 *
 * @author Steve Ebersole
 */
public class ClassLevelPropertyAccessTests {

	/**
	 * Tests implicit class-level PROPERTY access, based on `@Id` placement
	 */
	@Test
	void testImplicitPropertyAccess() {
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				ImplicitPropertyAccessEntity.class
		);

		final SourceModelBuildingContext modelBuildingContext = new SourceModelBuildingContextImpl( SIMPLE_CLASS_LOADING, jandexIndex );
		final ClassDetailsRegistry classDetailsRegistry = new ClassDetailsRegistryStandard( modelBuildingContext );
		final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( ImplicitPropertyAccessEntity.class.getName() );
		final List<MemberDetails> attributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				classDetails,
				ClassAttributeAccessType.IMPLICIT_PROPERTY,
				null
		);
		assertThat( attributeMembers ).hasSize( 2 );
		assertThat( attributeMembers.stream().map( MemberDetails::getName ) ).contains( "name", "getId" );
	}

	/**
	 * Test behavior of an explicit `@Access(FIELD)` on the class
	 */
	@Test
	void testExplicitPropertyAccess() {
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				ImplicitPropertyAccessEntity.class
		);

		final SourceModelBuildingContext modelBuildingContext = new SourceModelBuildingContextImpl( SIMPLE_CLASS_LOADING, jandexIndex );
		final ClassDetailsRegistry classDetailsRegistry = new ClassDetailsRegistryStandard( modelBuildingContext );
		final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( ImplicitPropertyAccessEntity.class.getName() );
		final List<MemberDetails> attributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				classDetails,
				ClassAttributeAccessType.EXPLICIT_PROPERTY,
				null
		);
		assertThat( attributeMembers ).hasSize( 2 );
		assertThat( attributeMembers.stream().map( MemberDetails::getName ) ).contains( "name", "getId" );
	}


	@Entity(name="ImplicitPropertyAccessEntity")
	public static class ImplicitPropertyAccessEntity {
		private Integer id;

		@Access(AccessType.FIELD)
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
}
