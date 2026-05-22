/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.exception;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractEntityInsertUniqueConstraintBatchingTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void uniqueConstraintViolationDuringBatchedEntityInsertBecomesEntityExistsException(
			EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new UniqueEntity( 1L, "duplicate" ) ) );

		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			try {
				entityManager.persist( new UniqueEntity( 2L, "duplicate" ) );

				assertThrows( EntityExistsException.class, entityManager::flush );
			}
			finally {
				entityManager.getTransaction().rollback();
			}
		} );
	}

	@Test
	public void uniqueConstraintViolationDuringBatchedCollectionInsertDoesNotBecomeEntityExistsException(
			EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CollectionOwner owner = new CollectionOwner( 1L );
			owner.tags.add( "duplicate" );
			entityManager.persist( owner );
		} );

		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			try {
				final CollectionOwner owner = new CollectionOwner( 2L );
				owner.tags.add( "duplicate" );
				entityManager.persist( owner );

				final PersistenceException exception = assertThrows( PersistenceException.class, entityManager::flush );
				assertThat( exception )
						.isInstanceOf( ConstraintViolationException.class )
						.isNotInstanceOf( EntityExistsException.class );
			}
			finally {
				entityManager.getTransaction().rollback();
			}
		} );
	}

	@Entity(name = "UniqueEntity")
	@Table(
			name = "jpa4_unique_entity",
			uniqueConstraints = @UniqueConstraint(columnNames = "code")
	)
	public static class UniqueEntity {
		@Id
		private Long id;

		@Column(nullable = false)
		private String code;

		protected UniqueEntity() {
		}

		private UniqueEntity(Long id, String code) {
			this.id = id;
			this.code = code;
		}
	}

	@Entity(name = "CollectionOwner")
	@Table(name = "jpa4_collection_owner")
	public static class CollectionOwner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(
				name = "jpa4_collection_owner_tag",
				joinColumns = @JoinColumn(name = "owner_id"),
				uniqueConstraints = @UniqueConstraint(columnNames = "tag")
		)
		@Column(name = "tag", nullable = false)
		private List<String> tags = new ArrayList<>();

		protected CollectionOwner() {
		}

		private CollectionOwner(Long id) {
			this.id = id;
		}
	}
}
