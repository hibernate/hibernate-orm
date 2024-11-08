/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.access;

import jakarta.persistence.*;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				InvalidPropertyNameTest.SomeEntity.class,
		}
)
@SessionFactory
@JiraKey("HHH-16572")
@BytecodeEnhanced
public class InvalidPropertyNameTest {


	@Test
	@FailureExpected(jiraKey = "HHH-16572")
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SomeEntity( 1L, "field", "property" ) );
		} );

		scope.inTransaction( session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: property" );

			entity.setPropertyMethod( "updated" );
		} );

		scope.inTransaction( session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: updated" );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		// uncomment the following when @FailureExpected is removed above
		// scope.inTransaction( session -> {
		//	session.remove( session.get( SomeEntity.class, 1L ) );
		// PropertyAccessTest} );
	}

	@Entity
	@Table(name = "SOME_ENTITY")
	static class SomeEntity {
		@Id
		Long id;

		@Basic
		String field;

		String property;

		public SomeEntity() {
		}

		public SomeEntity(Long id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

		/**
		 * The following property accessor methods are purposely named incorrectly to
		 * not match the "property" field.  The HHH-16572 change ensures that
		 * this entity is not (bytecode) enhanced.  Eventually further changes will be made
		 * such that this entity is enhanced in which case the FailureExpected can be removed
		 * and the cleanup() uncommented.
		 */
		@Basic
		@Access(AccessType.PROPERTY)
		public String getPropertyMethod() {
			return "from getter: " + property;
		}

		public void setPropertyMethod(String property) {
			this.property = property;
		}
	}
}
