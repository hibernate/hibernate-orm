/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		HierarchyPropertyAccessTest.AbstractSuperclass.class,
		HierarchyPropertyAccessTest.ParentEntity.class,
		HierarchyPropertyAccessTest.ChildEntity.class,
})
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-19140" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19059" )
@BytecodeEnhanced
public class HierarchyPropertyAccessTest {
	@Test
	public void testParent(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getMappingMetamodel().findEntityDescriptor( ParentEntity.class )
				.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ).isTrue();
		scope.inTransaction( session -> session.persist( new ParentEntity( 1L, "field", "transient: property" ) ) );

		scope.inTransaction( session -> {
			final ParentEntity entity = session.find( ParentEntity.class, 1L );
			assertThat( entity.getPersistProperty() ).isEqualTo( "property" );
			assertThat( entity.getProperty() ).isEqualTo( "transient: property" );
			assertThat( entity.getSuperProperty() ).isEqualTo( 8 );

			entity.setProperty( "transient: updated" );
		} );

		scope.inTransaction( session -> {
			final ParentEntity entity = session.find( ParentEntity.class, 1L );
			assertThat( entity.getPersistProperty() ).isEqualTo( "updated" );
			assertThat( entity.getProperty() ).isEqualTo( "transient: updated" );
		} );
	}

	@Test
	public void testChild(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getMappingMetamodel().findEntityDescriptor( ChildEntity.class )
				.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ).isTrue();
		scope.inTransaction( session -> session.persist( new ChildEntity( 2L, "field", "transient: property" ) ) );

		scope.inTransaction( session -> {
			ChildEntity entity = session.find( ChildEntity.class, 2L );
			assertThat( entity.getPersistProperty() ).isEqualTo( "property" );
			assertThat( entity.getProperty() ).isEqualTo( "transient: property" );
			assertThat( entity.getSuperProperty() ).isEqualTo( 8 );

			entity.setProperty( "transient: updated" );
		} );

		scope.inTransaction( session -> {
			ChildEntity entity = session.find( ChildEntity.class, 2L );
			assertThat( entity.getPersistProperty() ).isEqualTo( "updated" );
			assertThat( entity.getProperty() ).isEqualTo( "transient: updated" );
		} );
	}

	@AfterAll
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@MappedSuperclass
	static abstract class AbstractSuperclass {
		protected Integer superProperty;
	}

	@Entity(name = "ParentEntity")
	@DiscriminatorColumn(name = "entity_type")
	static class ParentEntity extends AbstractSuperclass {
		@Id
		private Long id;

		private String field;

		private String persistProperty;

		@Transient
		private String property;

		public ParentEntity() {
		}

		public ParentEntity(Long id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

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

		@Access(AccessType.PROPERTY)
		public Integer getSuperProperty() {
			return getPersistProperty().length();
		}

		public void setSuperProperty(Integer superProperty) {
			this.superProperty = superProperty;
		}
	}

	@Entity(name = "ChildEntity")
	static class ChildEntity extends ParentEntity {
		public ChildEntity() {
		}

		public ChildEntity(Long id, String field, String property) {
			super( id, field, property );
		}
	}
}
