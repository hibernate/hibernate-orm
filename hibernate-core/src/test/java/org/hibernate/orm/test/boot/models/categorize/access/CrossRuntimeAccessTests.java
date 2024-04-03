/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize.access;

import java.util.List;

import org.hibernate.boot.models.categorize.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.internal.ClassDetailsRegistryStandard;
import org.hibernate.models.internal.SourceModelBuildingContextImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.orm.test.boot.models.categorize.CategorizationTestsHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * Tests explicit `@Access` annotations referring to the other kind of member -<ul>
 *     <li>`@Access(FIELD)` on a getter(</li>
 *     <li>`@Access(PROPERTY)` on a field(</li>
 * </ul> *
 *
 * @author Steve Ebersole
 */
public class CrossRuntimeAccessTests {

	/**
	 * Tests implicit FIELD access and an explicit PROPERTY access
	 */
	@Test
	void testCrossRuntimeAccess() {
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				CrossRuntimeAccessEntity.class
		);

		final SourceModelBuildingContext modelBuildingContext = new SourceModelBuildingContextImpl( SIMPLE_CLASS_LOADING, jandexIndex );
		final ClassDetailsRegistry classDetailsRegistry = new ClassDetailsRegistryStandard( modelBuildingContext );
		final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( CrossRuntimeAccessEntity.class.getName() );
		final List<MemberDetails> attributeMembers = StandardPersistentAttributeMemberResolver.INSTANCE.resolveAttributesMembers(
				classDetails,
				// todo (7.0) : its not self-evident what the answer should be here.
				//		- see below
				ClassAttributeAccessType.IMPLICIT_PROPERTY,
				null
		);
		assertThat( attributeMembers ).hasSize( 3 );
		assertThat( attributeMembers.stream().map( MemberDetails::getName ) ).containsExactly( "id", "getName", "getAnotherName" );
	}

	@Test
	@ServiceRegistry
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testCategorization(ServiceRegistryScope scope) {
		final CategorizedDomainModel categorizedDomainModel = CategorizationTestsHelper.buildCategorizedDomainModel(
				scope,
				CrossRuntimeAccessEntity.class
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );
		categorizedDomainModel.forEachEntityHierarchy( (index, entityHierarchy) -> {
			final EntityTypeMetadata hierarchyRoot = entityHierarchy.getRoot();
			// todo (7.0) : its not self-evident what the answer should be here.
			//		- boils down to whether this describes (1) where to look for annotations or (2) runtime access
			//		- in other words, what's important here - where the annotation is placed or what its value is?
			//		- we want the value to indicate the PropertyAccessStrategy
			assertThat( hierarchyRoot.getClassLevelAccessType() ).isEqualTo( ClassAttributeAccessType.IMPLICIT_PROPERTY );
			assertThat( hierarchyRoot.getAttributes().stream().map( attributeMetadata -> attributeMetadata.getMember().getName() ) )
					.containsExactly( "id", "getName", "getAnotherName" );
		} );
	}

	@Entity(name="CrossRuntimeAccessEntity")
	public static class CrossRuntimeAccessEntity {
		@Id
		@Access( AccessType.PROPERTY )
		private Integer id;
		private String name;
		private String anotherName;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Access(AccessType.FIELD)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAnotherName() {
			return anotherName;
		}

		public void setAnotherName(String anotherName) {
			this.anotherName = anotherName;
		}
	}
}
