package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Table;
import jakarta.persistence.TransactionRequiredException;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Executable persistence-context synchronization examples using assigned identifiers.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = PersistenceContextFlushTests.Item.class)
public class PersistenceContextFlushTests {
	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void explicitSynchronization(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::pc-explicit-flush[]
			entityManager.setFlushMode( FlushModeType.EXPLICIT );
			Item item = new Item( 1L, "pending" );
			entityManager.persist( item );
			assertThat( entityManager.createQuery( "select count(i) from FlushGuideItem i", Long.class )
					.getSingleResult() ).isZero();

			entityManager.flush();
			assertThat( entityManager.createQuery( "select count(i) from FlushGuideItem i", Long.class )
					.getSingleResult() ).isEqualTo( 1L );
			//end::pc-explicit-flush[]
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Item.class, 1L ) ).isNotNull() );
	}

	@Test
	void explicitModeDoesNotFlushOnCommit(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.setFlushMode( FlushModeType.EXPLICIT );
			entityManager.persist( new Item( 1L, "pending" ) );
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Item.class, 1L ) ).isNull() );
	}

	@Test
	void queryOverride(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::pc-query-flush-override[]
			entityManager.setFlushMode( FlushModeType.COMMIT );
			entityManager.persist( new Item( 1L, "pending" ) );
			Long count = entityManager.createQuery( "select count(i) from FlushGuideItem i", Long.class )
					.setQueryFlushMode( QueryFlushMode.FLUSH )
					.getSingleResult();
			assertThat( count ).isEqualTo( 1L );
			//end::pc-query-flush-override[]
			assertThat( entityManager.getFlushMode() ).isEqualTo( FlushModeType.COMMIT );
		} );
	}

	@ParameterizedTest
	@EnumSource(QueryFlushMode.class)
	void nativeQueryOverridesAuto(QueryFlushMode mode, EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Item( 1L, "pending" ) );
			assertThat( count( entityManager, mode ) ).isEqualTo( mode == QueryFlushMode.NO_FLUSH ? 0L : 1L );
			assertThat( entityManager.getFlushMode() ).isEqualTo( FlushModeType.AUTO );
		} );
		// Suppressing query synchronization does not suppress synchronization on commit.
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Item.class, 1L ) ).isNotNull() );
	}

	@Test
	void queryCanFlushExplicitContext(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.setFlushMode( FlushModeType.EXPLICIT );
			entityManager.persist( new Item( 1L, "pending" ) );
			assertThat( count( entityManager, QueryFlushMode.FLUSH ) ).isEqualTo( 1L );
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Item.class, 1L ) ).isNotNull() );
	}

	@Test
	void queryDoesNotFlushOutsideTransaction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.persist( new Item( 1L, "pending" ) );
			assertThat( count( entityManager, QueryFlushMode.FLUSH ) ).isZero();
			assertThatThrownBy( entityManager::flush ).isInstanceOf( TransactionRequiredException.class );
		} );
	}

	@Test
	void clearCancelsPendingInsertion(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Item( 1L, "pending" ) );
			entityManager.clear();
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Item.class, 1L ) ).isNull() );
	}

	@Test
	void flushDoesNotCommit(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			try {
				entityManager.persist( new Item( 1L, "pending" ) );
				entityManager.flush();
				assertThat( count( entityManager, QueryFlushMode.NO_FLUSH ) ).isEqualTo( 1L );
			}
			finally {
				entityManager.getTransaction().rollback();
			}
		} );
		scope.inTransaction( entityManager -> assertThat( entityManager.find( Item.class, 1L ) ).isNull() );
	}

	private static long count(EntityManager entityManager, QueryFlushMode mode) {
		return entityManager.createNativeQuery( "select count(*) from guide_flush_item", Long.class )
				.setQueryFlushMode( mode )
				.getSingleResult();
	}

	@Entity(name = "FlushGuideItem")
	@Table(name = "guide_flush_item")
	public static class Item {
		@Id
		Long id;
		String name;

		public Item() {
		}

		Item(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
