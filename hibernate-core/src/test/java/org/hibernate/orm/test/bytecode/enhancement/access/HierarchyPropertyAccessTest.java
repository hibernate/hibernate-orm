/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.access;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				HierarchyPropertyAccessTest.ChildEntity.class,
		}
)
@SessionFactory
@JiraKey("HHH-19140")
@BytecodeEnhanced
public class HierarchyPropertyAccessTest {


	@Test
	public void testParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ParentEntity( 1L, "field", "transient: property" ) );
		} );

		scope.inTransaction( session -> {
			ParentEntity entity = session.get( ParentEntity.class, 1L );
			assertThat( entity.persistProperty ).isEqualTo( "property" );
			assertThat( entity.property ).isEqualTo( "transient: property" );

			entity.setProperty( "transient: updated" );
		} );

		scope.inTransaction( session -> {
			ParentEntity entity = session.get( ParentEntity.class, 1L );
			assertThat( entity.persistProperty ).isEqualTo( "updated" );
			assertThat( entity.property ).isEqualTo( "transient: updated" );
		} );
	}

	@Test
	public void testChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ChildEntity( 2L, "field", "transient: property" ) );
		} );

		scope.inTransaction( session -> {
			ChildEntity entity = session.get( ChildEntity.class, 2L );
			assertThat( entity.persistProperty ).isEqualTo( "property" );
			assertThat( entity.property ).isEqualTo( "transient: property" );

			entity.setProperty( "transient: updated" );
		} );

		scope.inTransaction( session -> {
			ChildEntity entity = session.get( ChildEntity.class, 2L );
			assertThat( entity.persistProperty ).isEqualTo( "updated" );
			assertThat( entity.property ).isEqualTo( "transient: updated" );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ParentEntity parentEntity = session.get( ParentEntity.class, 1L );
			if (parentEntity != null) {
				session.remove( parentEntity );
			}
			ChildEntity childEntity = session.get( ChildEntity.class, 2L );
			if (childEntity != null) {
				session.remove( childEntity );
			}
		} );
	}

	@Entity
	@Table(name = "PARENT_ENTITY")
	@Inheritance
	@DiscriminatorColumn(name = "type")
	@DiscriminatorValue("Parent")
	static class ParentEntity {
		@Id
		Long id;

		@Basic
		String field;

		String persistProperty;

		@Transient
		String property;

		public ParentEntity() {
		}

		public ParentEntity(Long id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

		@Basic
		@Access(AccessType.PROPERTY)
		public String getPersistProperty() {
			this.persistProperty = this.property.substring( 11 );
			return this.persistProperty;
		}

		public void setPersistProperty(String persistProperty) {
			this.property = "transient: " + persistProperty;
			this.persistProperty = persistProperty;
		}

		public String getProperty() {
			return this.property;
		}

		public void setProperty(String property) {
			this.property = property;
		}
	}

	@Entity
	@DiscriminatorValue("Child")
	static class ChildEntity extends ParentEntity {

		public ChildEntity() {
		}

		public ChildEntity(Long id, String field, String property) {
			super(id, field, property);
		}
	}
}
