/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.mappedBy;

import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManyToManyMappedByTypeTest {
	@Test
	public void testCorrect() {
		try (StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( EntityACorrect.class )
					.addAnnotatedClass( EntityBCorrect.class )
					.addAnnotatedClass( EntityC.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Test
	public void testWrong() {
		try (StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( EntityAWrong.class )
					.addAnnotatedClass( EntityBWrong.class )
					.addAnnotatedClass( EntityC.class );
			final AnnotationException thrown = assertThrows( AnnotationException.class, metadataSources::buildMetadata );
			assertTrue( thrown.getMessage().contains( "'parents' which references the wrong entity type" ) );
		}
	}

	@Test
	public void testCorrectSuperclass() {
		try (StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( SuperclassEntity.class )
					.addAnnotatedClass( SubclassEntity.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Entity( name = "EntityACorrect" )
	public static class EntityACorrect {
		@Id
		private Long id;

		@ManyToMany( mappedBy = "parents" )
		private List<EntityBCorrect> children;
	}

	@Entity( name = "EntityBCorrect" )
	public static class EntityBCorrect {
		@Id
		private Long id;

		@ManyToMany
		private List<EntityACorrect> parents;
	}

	@Entity( name = "EntityAWrong" )
	public static class EntityAWrong {
		@Id
		private Long id;

		@ManyToMany( mappedBy = "parents" )
		private List<EntityBWrong> children;
	}

	@Entity( name = "EntityBWrong" )
	public static class EntityBWrong {
		@Id
		private Long id;

		@ManyToMany
		private List<EntityC> parents;
	}

	@Entity( name = "EntityC" )
	public static class EntityC {
		@Id
		private Long id;
	}

	@Entity( name = "SubclassEntity" )
	public static class SubclassEntity extends SuperclassEntity {
		@ManyToMany( mappedBy = "parents" )
		private List<SuperclassEntity> children;
	}

	@Entity( name = "SuperclassEntity" )
	public static class SuperclassEntity {
		@Id
		private Long id;

		@ManyToMany
		private List<SuperclassEntity> parents;
	}
}
