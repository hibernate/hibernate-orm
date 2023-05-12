/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.bytecode.bytebuddy;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceHelper;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class ClassLevelAccessTests {
	@Test
	void testClassLevelAccessEntity() {
		Helper.withProcessingContext( (processingContext) -> {
			final ClassDetails classDetails = processingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( ClassLevelAccessEntity.class.getName() );
			assertThat( classDetails ).isNotNull();
			assertThat( classDetails.getSuperType() ).isNull();

			final Access classLevelAccessAnn = classDetails.getAnnotation( Access.class );
			assertThat( classLevelAccessAnn ).isNotNull();

			// id, name
			assertThat( classDetails.getFields() ).hasSize( 2 );
			// getId, getName, setName
			assertThat( classDetails.getMethods() ).hasSize( 3 );

			final AccessType determinedAccessType = ModelSourceHelper.determineClassLevelAccessType(
					classDetails,
					classDetails.getIdentifierMember(),
					null
			);
			assertThat( determinedAccessType ).isEqualTo( AccessType.FIELD );
		} );
	}

	@Entity
	@Access( AccessType.FIELD )
	public static class ClassLevelAccessEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		protected ClassLevelAccessEntity() {
			// for Hibernate use
		}

		public ClassLevelAccessEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
