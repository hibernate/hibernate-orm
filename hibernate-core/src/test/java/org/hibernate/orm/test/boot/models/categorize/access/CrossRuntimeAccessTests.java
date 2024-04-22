/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize.access;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.orm.test.boot.models.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.orm.test.boot.models.categorize.CategorizationTestsHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static jakarta.persistence.AccessType.FIELD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests explicit `@Access` annotations referring to the other kind of member -<ul>
 *     <li>`@Access(FIELD)` on a getter(</li>
 *     <li>`@Access(PROPERTY)` on a field(</li>
 * </ul> *
 *
 * @author Steve Ebersole
 */
public class CrossRuntimeAccessTests {

	@Test
	@ServiceRegistry
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testCategorization(ServiceRegistryScope scope) {
		final CategorizedDomainModel categorizedDomainModel = CategorizationTestsHelper.buildCategorizedDomainModel(
				scope,
				CrossRuntimeAccessEntity.class
		);

		assertThat( categorizedDomainModel.getEntityHierarchyCollection().getHierarchies() ).hasSize( 1 );
		categorizedDomainModel.forEachEntityHierarchy( (index, entityHierarchy) -> {
			final EntityTypeMetadata hierarchyRoot = entityHierarchy.getRoot();

			//the default access type for the hierarchy is FIELD because `@Id` is on the field
			assertThat( entityHierarchy.getDefaultAccessType() ).isEqualTo( FIELD );
			assertThat( hierarchyRoot.getClassLevelAccessType() ).isEqualTo( ClassAttributeAccessType.IMPLICIT_FIELD );

			// we should have 3 attributes with the 3 fields as the "backing member" in terms of where to look for annotations.
			assertThat( hierarchyRoot.getAttributes().stream().map( attributeMetadata -> attributeMetadata.getMember().getName() ) )
					.containsExactly( "getName", "id", "anotherName" );

			final AttributeMetadata attribute = hierarchyRoot.findAttribute( "anotherName" );
			final AnnotationUsage<Column> columnUsage = attribute.getMember().getAnnotationUsage( Column.class );
			assertThat( columnUsage.getString( "name" ) ).isEqualTo( "bite_the_dust" );
		} );
	}

	@Entity(name="CrossRuntimeAccessEntity")
	public static class CrossRuntimeAccessEntity {
		@Id
		@Access( AccessType.PROPERTY )
		private Integer id;
		private String name;
		@Column(name="bite_the_dust")
		private String anotherName;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Access(FIELD)
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
