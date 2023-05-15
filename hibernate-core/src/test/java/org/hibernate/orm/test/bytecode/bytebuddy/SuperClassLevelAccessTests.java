/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.bytecode.bytebuddy;

import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MemberDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceHelper;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.PersistentAttribute;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SuperClassLevelAccessTests {
	@Test
	void testClassDetailsBuilding() {
		Helper.withProcessingContext( (processingContext) -> {
			final ClassDetails classDetails = processingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( SuperClassLevelAccessEntity.class.getName() );
			assertThat( classDetails ).isNotNull();

			// SuperClassLevelAccessEntitySuperClass should have been processed when processing the sub.
			// `#getClassDetails` will throw an exception if it is not already there
			final ClassDetails superClassDetails = processingContext
					.getClassDetailsRegistry()
					.getClassDetails( SuperClassLevelAccessEntitySuperClass.class.getName() );
			// make doubly sure
			assertThat( superClassDetails ).isNotNull();
			// and
			assertThat( classDetails.getSuperType() ).isNotNull();

			final Access classLevelAccessAnn = classDetails.getAnnotation( Access.class );
			assertThat( classLevelAccessAnn ).isNull();
			final Access superClassLevelAccessAnn = superClassDetails.getAnnotation( Access.class );
			assertThat( superClassLevelAccessAnn ).isNotNull();

			// id, name
			assertThat( classDetails.getFields() ).hasSize( 2 );
			// getId, getName, setName
			assertThat( classDetails.getMethods() ).hasSize( 3 );

			final LinkedHashMap<String, MemberDetails> backingMembers = ModelSourceHelper.collectBackingMembers(
					classDetails,
					AccessType.PROPERTY
			);

			assertThat( backingMembers ).hasSize( 2 );

			final AccessType determinedAccessType = ModelSourceHelper.determineClassLevelAccessType(
					classDetails,
					classDetails.getIdentifierMember(),
					null
			);
			assertThat( determinedAccessType ).isEqualTo( AccessType.FIELD );
		} );
	}

	@Test
	void testPersistentAttributeResolution() {
		Helper.withProcessingContext( (modelProcessingContext) -> {
			final ClassDetails classDetails = modelProcessingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( SuperClassLevelAccessEntity.class.getName() );
			final List<PersistentAttribute> persistentAttributes = ModelSourceHelper.buildPersistentAttributeList(
					classDetails,
					null,
					modelProcessingContext
			);
			assertThat( persistentAttributes ).hasSize( 2 );
			assertThat( persistentAttributes.get( 0 ).getAccessType() ).isEqualTo( AccessType.FIELD );
			assertThat( persistentAttributes.get( 1 ).getAccessType() ).isEqualTo( AccessType.FIELD );
		} );
	}

	@Entity
	public static class SuperClassLevelAccessEntity extends SuperClassLevelAccessEntitySuperClass {
		@Id
		private Integer id;
		@Basic
		private String name;

		protected SuperClassLevelAccessEntity() {
			// for Hibernate use
		}

		public SuperClassLevelAccessEntity(Integer id, String name) {
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

	@MappedSuperclass
	@Access( AccessType.FIELD )
	public static class SuperClassLevelAccessEntitySuperClass {
	}
}
