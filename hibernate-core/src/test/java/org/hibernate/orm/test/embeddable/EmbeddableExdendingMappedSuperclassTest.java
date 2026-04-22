/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

// It's important for the test that embeddables and mapped superclass are listed in the @Jpa annotatedClasses.
@Jpa(
		annotatedClasses = {
				EmbeddableExdendingMappedSuperclassTest.DescriptionMappedSuperclass.class,
				EmbeddableExdendingMappedSuperclassTest.Item.class,
				EmbeddableExdendingMappedSuperclassTest.Item2.class,
				EmbeddableExdendingMappedSuperclassTest.DescriptionBaseEmbeddable.class,
				EmbeddableExdendingMappedSuperclassTest.DescriptionBase.class

		}
)
@JiraKey("HHH-20356")
public class EmbeddableExdendingMappedSuperclassTest {

	@Test
	public void testSetDescriptionMappedSuperclass(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 1L, new DescriptionMappedSuperclass( "Test2" ) ) )
		);
	}

	@Test
	public void testSetDescriptionBase(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item( 2L, new DescriptionBase( "Test" ) ) )
		);
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 2L, new DescriptionBase( "Test2" ) ) )
		);
	}

	@Test
	public void testSetDescriptionBaseEmbeddable(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item( 3L, new DescriptionBaseEmbeddable( "Test" ) ) )
		);
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 3L, new DescriptionBaseEmbeddable( "Test2" ) ) )
		);
	}

	@Test
	public void testSetDescriptionEmbeddable(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 4L, new DescriptionEmbeddable( "Test2" ) ) )
		);
	}

	@Entity(name = "Item")
	public static class Item {

		@Id
		private Long id;

		@Embedded
		private DescriptionBase description;

		protected Item() {
		}

		public Item(Long id, DescriptionBase description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public DescriptionBase getDescription() {
			return description;
		}
	}

	@Entity(name = "Item2")
	public static class Item2 {

		@Id
		private Long id;

		@Embedded
		private DescriptionMappedSuperclass description;

		protected Item2() {
		}

		public Item2(Long id, DescriptionMappedSuperclass description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public DescriptionMappedSuperclass getDescription() {
			return description;
		}
	}

	@MappedSuperclass
	public static class DescriptionMappedSuperclass {

		private String name;

		public DescriptionMappedSuperclass() {
		}

		public DescriptionMappedSuperclass(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class DescriptionEmbeddable extends DescriptionMappedSuperclass {

		public DescriptionEmbeddable() {
			super();
		}

		public DescriptionEmbeddable(String name) {
			super( name );
		}
	}

	public static class DescriptionBase extends DescriptionMappedSuperclass {
		public DescriptionBase() {
			super();
		}

		public DescriptionBase(String name) {
			super( name );
		}
	}

	@Embeddable
	public static class DescriptionBaseEmbeddable extends DescriptionBase {

		public DescriptionBaseEmbeddable() {
			super();
		}

		public DescriptionBaseEmbeddable(String name) {
			super( name );
		}
	}
}
