/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.bytecode.bytebuddy;

import java.util.List;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.AnnotationTarget;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceHelper;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.PersistentAttribute;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class OddNamingTests {

	@Test
	void testClassDetailsBuilding() {
		Helper.withProcessingContext( (processingContext) -> {
			final ClassDetails classDetails = processingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( OddNamingEntity.class.getName() );
			assertThat( classDetails ).isNotNull();
			assertThat( classDetails.getSuperType() ).isNull();

			final Access classLevelAccessAnn = classDetails.getAnnotation( Access.class );
			assertThat( classLevelAccessAnn ).isNull();

			assertThat( classDetails.getIdentifierMember() ).isNotNull();
			assertThat( classDetails.getIdentifierMember().getKind() ).isEqualTo( AnnotationTarget.Kind.METHOD );

			final AccessType determinedAccessType = ModelSourceHelper.determineClassLevelAccessType(
					classDetails,
					classDetails.getIdentifierMember(),
					null
			);
			assertThat( determinedAccessType ).isEqualTo( AccessType.PROPERTY );

			final AccessType determinedAccessTypeLegacy = ModelSourceHelper.determineClassLevelAccessType(
					classDetails,
					null,
					null
			);
			assertThat( determinedAccessTypeLegacy ).isNull();

			// id, name
			assertThat( classDetails.getFields() ).hasSize( 2 );
			// getId, setId, getPrimaryName, setPrimaryName
			assertThat( classDetails.getMethods() ).hasSize( 4 );
		} );
	}

	@Test
	void testPersistentAttributeResolution() {
		Helper.withProcessingContext( (modelProcessingContext) -> {
			final ClassDetails classDetails = modelProcessingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( OddNamingEntity.class.getName() );
			final List<PersistentAttribute> persistentAttributes = ModelSourceHelper.buildPersistentAttributeList(
					classDetails,
					null,
					modelProcessingContext
			);
			assertThat( persistentAttributes ).hasSize( 2 );
			assertThat( persistentAttributes.get( 0 ).getAccessType() ).isEqualTo( AccessType.PROPERTY );
			assertThat( persistentAttributes.get( 1 ).getAccessType() ).isEqualTo( AccessType.PROPERTY );
		} );
	}

	@Entity
	public static class OddNamingEntity {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getPrimaryName() {
			return name;
		}

		public void setPrimaryName(String name) {
			this.name = name;
		}
	}
}
