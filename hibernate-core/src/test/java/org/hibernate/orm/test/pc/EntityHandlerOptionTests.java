/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.BatchSize;
import org.hibernate.CacheMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.SubselectFetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
@SessionFactory
public class EntityHandlerOptionTests {
	@Test
	void testReadOnlyModeSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat(em.isDefaultReadOnly()).isFalse();
		}

		try (var em = sf.createEntityManager( ReadOnlyMode.READ_ONLY )) {
			assertThat(em.isDefaultReadOnly()).isTrue();
		}

		try (var em = sf.createEntityManager()) {
			assertThat(em.isDefaultReadOnly()).isFalse();
			em.addOption( ReadOnlyMode.READ_ONLY );
			assertThat(em.isDefaultReadOnly()).isTrue();
		}
	}

	@Test
	void testBatchSizeSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat(em.getJdbcBatchSize()).isNull();
		}

		try (var em = sf.createEntityManager(new BatchSize( 10 ) )) {
			assertThat(em.getJdbcBatchSize()).isEqualTo(10);
		}

		try (var em = sf.createEntityManager()) {
			em.addOption( new BatchSize( 20 ) );
			assertThat(em.getJdbcBatchSize()).isEqualTo(20);
		}
	}

	@Test
	void testBatchSizeStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityAgent()) {
			assertThat(em.getJdbcBatchSize()).isEqualTo(0);
		}

		try (var em = sf.createEntityAgent(new BatchSize( 10 ) )) {
			assertThat(em.getJdbcBatchSize()).isEqualTo(10);
		}

		try (var em = sf.createEntityAgent()) {
			em.addOption( new BatchSize( 20 ) );
			assertThat(em.getJdbcBatchSize()).isEqualTo(20);
		}
	}

	@Test
	void testCacheModeSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat(em.getCacheMode()).isEqualTo( CacheMode.NORMAL );
		}

		try (var em = sf.createEntityManager( CacheMode.IGNORE )) {
			assertThat(em.getCacheMode()).isEqualTo( CacheMode.IGNORE );
		}

		try (var em = sf.createEntityManager()) {
			em.addOption( CacheMode.IGNORE );
			assertThat(em.getCacheMode()).isEqualTo( CacheMode.IGNORE );
		}
	}

	@Test
	void testCacheModeStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityAgent()) {
			assertThat(em.getCacheMode()).isEqualTo( CacheMode.NORMAL );
		}

		try (var em = sf.createEntityAgent( CacheMode.IGNORE )) {
			assertThat(em.getCacheMode()).isEqualTo( CacheMode.IGNORE );
		}

		try (var em = sf.createEntityAgent()) {
			em.addOption( CacheMode.IGNORE );
			assertThat(em.getCacheMode()).isEqualTo( CacheMode.IGNORE );
		}
	}

	@Test
	void testSubselectFetchModeSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat(em.isSubselectFetchingEnabled()).isFalse();
		}

		try (var em = sf.createEntityManager( SubselectFetchMode.ENABLED)) {
			assertThat(em.isSubselectFetchingEnabled()).isTrue();
		}
	}

	@Entity
	public static class TestEntity {
		@Id
		private Long id;
		private String name;
	}
}
