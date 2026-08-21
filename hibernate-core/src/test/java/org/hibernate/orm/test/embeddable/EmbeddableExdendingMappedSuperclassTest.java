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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		verifyItem2( scope, 1L, "Test2" );
	}

	@Test
	public void testSetDescriptionBase(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item( 2L, new DescriptionBase( "Test", "base1" ) ) )
		);
		verifyItem( scope, 2L, "Test", "base1" );
		// Item2's embedded type is DescriptionMappedSuperclass, so baseDetail is not persistent
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 2L, new DescriptionBase( "Test2", "base2" ) ) )
		);
		verifyItem2( scope, 2L, "Test2" );
	}

	@Test
	public void testSetDescriptionBaseEmbeddable(EntityManagerFactoryScope scope) {
		// Item's embedded type is DescriptionBase, so baseEmbeddableDetail is not persistent
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item( 3L, new DescriptionBaseEmbeddable( "Test", "base1", "baseEmb1" ) ) )
		);
		verifyItem( scope, 3L, "Test", "base1" );
		// Item2's embedded type is DescriptionMappedSuperclass, so baseDetail and baseEmbeddableDetail are not persistent
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 3L, new DescriptionBaseEmbeddable( "Test2", "base2", "baseEmb2" ) ) )
		);
		verifyItem2( scope, 3L, "Test2" );
	}

	@Test
	public void testSetDescriptionEmbeddable(EntityManagerFactoryScope scope) {
		// Item2's embedded type is DescriptionMappedSuperclass, so embeddableDetail is not persistent
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item2( 4L, new DescriptionEmbeddable( "Test2", "emb1" ) ) )
		);
		verifyItem2( scope, 4L, "Test2" );
	}

	private void verifyItem(EntityManagerFactoryScope scope, Long id, String expectedName, String expectedBaseDetail) {
		scope.inTransaction( entityManager -> {
			final Item item = entityManager.find( Item.class, id );
			assertEquals( expectedName, item.getDescription().getName() );
			assertEquals( expectedBaseDetail, item.getDescription().getBaseDetail() );
		} );
	}

	private void verifyItem2(EntityManagerFactoryScope scope, Long id, String expectedName) {
		scope.inTransaction( entityManager -> {
			final Item2 item = entityManager.find( Item2.class, id );
			assertEquals( expectedName, item.getDescription().getName() );
		} );
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

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public static class DescriptionEmbeddable extends DescriptionMappedSuperclass {
		private String embeddableDetail;

		public DescriptionEmbeddable() {
			super();
		}

		public DescriptionEmbeddable(String name, String embeddableDetail) {
			super( name );
			this.embeddableDetail = embeddableDetail;
		}
	}

	public static class DescriptionBase extends DescriptionMappedSuperclass {
		private String baseDetail;

		public DescriptionBase() {
			super();
		}

		public DescriptionBase(String name, String baseDetail) {
			super( name );
			this.baseDetail = baseDetail;
		}

		public String getBaseDetail() {
			return baseDetail;
		}
	}

	@Embeddable
	public static class DescriptionBaseEmbeddable extends DescriptionBase {
		private String baseEmbeddableDetail;

		public DescriptionBaseEmbeddable() {
			super();
		}

		public DescriptionBaseEmbeddable(String name, String baseDetail, String baseEmbeddableDetail) {
			super( name, baseDetail );
			this.baseEmbeddableDetail = baseEmbeddableDetail;
		}
	}
}
