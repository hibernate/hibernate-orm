/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mappedBy;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneToOneMappedByTypeTest {
	@Test
	public void testCorrect() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( EntityACorrect.class )
					.addAnnotatedClass( EntityBCorrect.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Test
	public void testWrong() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( EntityAWrong.class )
					.addAnnotatedClass( EntityBWrong.class )
					.addAnnotatedClass( EntityC.class );
			final AnnotationException thrown = assertThrows( AnnotationException.class, metadataSources::buildMetadata );
			assertTrue( thrown.getMessage().contains( "'parent' which references the wrong entity type" ) );
		}
	}

	@Test
	public void testCorrectSuperclass() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( SuperclassEntity.class )
					.addAnnotatedClass( SubclassEntity.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Test
	public void testCorrectSameTable() {
		// Allow different entity types which map to the same table since the mappedBy
		// in that case would still make sense from a database perspective
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( EntityACorrect.class )
					.addAnnotatedClass( EntityBCorrect.class )
					.addAnnotatedClass( EntityA2Correct.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Test
	public void testCorrectSubtype() {
		// Allow mappedBy subtypes given that users might want to filter the
		// association with custom @Where annotations and still use a supertype
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( EntityASupertype.class )
					.addAnnotatedClass( EntityAMappedSuperclass.class )
					.addAnnotatedClass( EntityASubtype.class )
					.addAnnotatedClass( EntityBSubtype.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Entity( name = "EntityACorrect" )
	@Table( name = "entity_a_correct" )
	public static class EntityACorrect {
		@Id
		private Long id;

		@OneToOne( mappedBy = "parent" )
		private EntityBCorrect child;
	}

	@Entity( name = "EntityBCorrect" )
	public static class EntityBCorrect {
		@Id
		private Long id;

		@OneToOne
		private EntityACorrect parent;
	}

	@Entity( name = "EntityA2Correct" )
	@Table( name = "entity_a_correct" )
	public static class EntityA2Correct {
		@Id
		private Long id;

		@OneToOne( mappedBy = "parent" )
		private EntityBCorrect child;
	}

	@Entity( name = "EntityASupertype" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class EntityASupertype {
		@Id
		private Long id;

		@OneToOne( mappedBy = "parent" )
		private EntityBSubtype child;
	}

	@MappedSuperclass
	public static class EntityAMappedSuperclass extends EntityASupertype {
	}

	@Entity( name = "EntityASubtype" )
	public static class EntityASubtype extends EntityAMappedSuperclass {
	}

	@Entity( name = "EntityBSubtype" )
	public static class EntityBSubtype {
		@Id
		private Long id;

		@OneToOne
		private EntityASubtype parent;
	}

	@Entity( name = "EntityAWrong" )
	public static class EntityAWrong {
		@Id
		private Long id;

		@OneToOne( mappedBy = "parent" )
		private EntityBWrong child;
	}

	@Entity( name = "EntityBWrong" )
	public static class EntityBWrong {
		@Id
		private Long id;

		@OneToOne
		private EntityC parent;
	}

	@Entity( name = "EntityC" )
	public static class EntityC {
		@Id
		private Long id;
	}

	@Entity( name = "SubclassEntity" )
	public static class SubclassEntity extends SuperclassEntity {
		@OneToOne( mappedBy = "parent" )
		private SuperclassEntity child;
	}

	@Entity( name = "SuperclassEntity" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class SuperclassEntity {
		@Id
		private Long id;

		@OneToOne
		private SuperclassEntity parent;
	}
}
