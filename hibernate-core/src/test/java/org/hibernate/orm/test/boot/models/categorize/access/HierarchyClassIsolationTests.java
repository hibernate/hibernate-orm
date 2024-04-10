/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize.access;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.boot.models.categorize.spi.PersistentAttributeMemberResolver;
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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import static jakarta.persistence.AccessType.PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * According to JPA `@Access` on a class only effects that class
 * and no others within a hierarchy.  Test that interpretation.
 * <p/>
 * The default implicit access-type for the hierarchy is based on
 * the placement of the `@Id`.
 * <p/>
 * Thus, the class-level access type for each class in the hierarchy is as follows: <ol>
 *     <li>FIELD for Root, the default for the hierarchy</li>
 *     <li>PROPERTY for Base because of the explicit `@Access`</li>
 *     <li>FIELD for Level1, the default for the hierarchy</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class HierarchyClassIsolationTests {
	/**
	 *
	 * Tests implicit class-level FIELD access, based on `@Id` placement
	 */
	@Test
	void testImplicitFieldAccess() {
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				Root.class,
				Base.class,
				Level1.class
		);

		final SourceModelBuildingContext modelBuildingContext = new SourceModelBuildingContextImpl( SIMPLE_CLASS_LOADING, jandexIndex );
		final ClassDetailsRegistry classDetailsRegistry = new ClassDetailsRegistryStandard( modelBuildingContext );

		final ClassDetails rootClassDetails = classDetailsRegistry.resolveClassDetails( Root.class.getName() );
		final List<MemberDetails> rootAttributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				rootClassDetails,
				ClassAttributeAccessType.IMPLICIT_FIELD,
				null
		);
		assertThat( rootAttributeMembers ).hasSize( 2 );
		assertThat( rootAttributeMembers.stream().map( MemberDetails::getName ) ).contains( "id", "name" );

		final ClassDetails baseClassDetails = classDetailsRegistry.resolveClassDetails( Base.class.getName() );
		final List<MemberDetails> baseAttributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				baseClassDetails,
				ClassAttributeAccessType.EXPLICIT_PROPERTY,
				null
		);
		assertThat( baseAttributeMembers ).hasSize( 1 );
		assertThat( baseAttributeMembers.stream().map( MemberDetails::getName ) ).contains( "getAnotherName" );

		final ClassDetails level1ClassDetails = classDetailsRegistry.resolveClassDetails( Level1.class.getName() );
		final List<MemberDetails> level1AttributeMembers = PersistentAttributeMemberResolver.STANDARD.resolveAttributesMembers(
				level1ClassDetails,
				ClassAttributeAccessType.IMPLICIT_FIELD,
				null
		);
		assertThat( level1AttributeMembers ).hasSize( 1 );
		assertThat( level1AttributeMembers.stream().map( MemberDetails::getName ) ).contains( "yetAnother" );
	}

	@Test @SuppressWarnings("JUnitMalformedDeclaration")
	@ServiceRegistry
	void testCategorization(ServiceRegistryScope scope) {
		final CategorizedDomainModel categorizedDomainModel = CategorizationTestsHelper.buildCategorizedDomainModel( scope, Root.class, Base.class, Level1.class );

		final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();
		final EntityTypeMetadata rootEntityDescriptor = entityHierarchy.getRoot();

		assertThat( rootEntityDescriptor.getClassLevelAccessType() ).isEqualTo( ClassAttributeAccessType.IMPLICIT_FIELD );
		assertThat( rootEntityDescriptor.getAttributes().stream().map( attributeMetadata -> attributeMetadata.getMember().getName() ) )
				.contains( "id", "name" );

		assertThat( rootEntityDescriptor.getNumberOfSubTypes() ).isEqualTo( 1 );
		rootEntityDescriptor.forEachSubType( (baseEntityDescriptor) -> {
			assertThat( baseEntityDescriptor.getClassLevelAccessType() ).isEqualTo( ClassAttributeAccessType.EXPLICIT_PROPERTY );
			assertThat( baseEntityDescriptor.getAttributes().stream().map( attributeMetadata -> attributeMetadata.getMember().getName() ) )
					.contains( "getAnotherName" );

			assertThat( baseEntityDescriptor.getNumberOfSubTypes() ).isEqualTo( 1 );
			baseEntityDescriptor.forEachSubType( (level1EntityDescriptor) -> {
				assertThat( level1EntityDescriptor.getClassLevelAccessType() ).isEqualTo( ClassAttributeAccessType.IMPLICIT_FIELD );
				assertThat( level1EntityDescriptor.getAttributes().stream().map( attributeMetadata -> attributeMetadata.getMember().getName() ) )
						.contains( "yetAnother" );
			} );
		} );

		final KeyMapping idMapping = entityHierarchy.getIdMapping();
		assertThat( idMapping.getKeyType().toJavaClass() ).isEqualTo( Integer.class );
		assertThat( idMapping.contains( rootEntityDescriptor.findAttribute( "id" ) ) ).isTrue();
		assertThat( idMapping.contains( rootEntityDescriptor.findAttribute( "name" ) ) ).isFalse();
	}

	@Entity(name="Root")
	@Inheritance
	public static class Root {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Base")
	@Access(PROPERTY)
	public static class Base extends Root {
		private String anotherName;

		@Column(name="more")
		public String getAnotherName() {
			return anotherName;
		}

		public void setAnotherName(String anotherName) {
			this.anotherName = anotherName;
		}
	}

	@Entity(name="Level1")
	public static class Level1 extends Base {
		@Column(name="yet_another")
		private String yetAnother;
	}
}
