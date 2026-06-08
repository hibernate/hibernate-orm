/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.BatchSize;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.ReadOnlyMode;
import org.hibernate.SessionCreationOption;
import org.hibernate.annotations.TenantId;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class EntityHandlerOptionTests {
	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
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
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
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
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
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
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
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
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
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
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testSubselectFetchModeSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat(em.isSubselectFetchingEnabled()).isFalse();
		}

		try (var em = sf.createEntityManager( SessionCreationOption.SubselectFetchMode.ENABLED)) {
			assertThat(em.isSubselectFetchingEnabled()).isTrue();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testEffectiveInstantSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat( em.unwrap( SessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNull();
		}

		try (var em = sf.createEntityManager(new SessionCreationOption.EffectiveAt( Instant.now()))) {
			assertThat( em.unwrap( SessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNotNull();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testEffectiveInstantStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityAgent()) {
			assertThat( em.unwrap( StatelessSessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNull();
		}

		try (var em = sf.createEntityAgent(new SessionCreationOption.EffectiveAt( Instant.now()))) {
			assertThat( em.unwrap( StatelessSessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNotNull();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testEffectiveChangesetSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			assertThat( em.unwrap( SessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNull();
		}

		try (var em = sf.createEntityManager(new SessionCreationOption.EffectiveChangeset( 123))) {
			assertThat( em.unwrap( SessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNotNull();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testEffectiveChangesetStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityAgent()) {
			assertThat( em.unwrap( StatelessSessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNull();
		}

		try (var em = sf.createEntityAgent(new SessionCreationOption.EffectiveChangeset( 123))) {
			assertThat( em.unwrap( StatelessSessionImplementor.class ).getLoadQueryInfluencers().getTemporalIdentifier() ).isNotNull();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity2.class)
	@SessionFactory
	void testTenantIdSession(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityManager()) {
			fail("Expecting an exception here");
		}
		catch (HibernateException e) {
			assertThat(e).hasMessageContaining( "multi-tenancy" );
		}

		try (var em = sf.createEntityManager( new SessionCreationOption.TenantId( "steve" ))) {
			assertThat(em.getTenantIdentifierValue()).isEqualTo( "steve" );
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity2.class)
	@SessionFactory
	void testTenantIdStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();

		try (var em = sf.createEntityAgent()) {
			fail("Expecting an exception here");
		}
		catch (HibernateException e) {
			assertThat(e).hasMessageContaining( "multi-tenancy" );
		}

		try (var em = sf.createEntityAgent( new SessionCreationOption.TenantId( "steve" ))) {
			assertThat(em.getTenantIdentifierValue()).isEqualTo( "steve" );
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testObserverStateful(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();
		var observer = new CollectingStatementObserver();

		try (var em = sf.createEntityManager( observer )) {
			em.inTransaction( (t) -> {
				assertThat( observer.getSqlQueries() ).hasSize( 0 );
				em.persist( new TestEntity( 1, "Stuff" ) );
			} );
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
		}
		finally {
			factoryScope.dropData();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testObserverStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();
		var observer = new CollectingStatementObserver();

		try (var em = sf.createEntityAgent( observer )) {
			em.inTransaction( (t) -> {
				assertThat( observer.getSqlQueries() ).hasSize( 0 );
				em.insert( new TestEntity( 1, "Stuff" ) );
			} );
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
		}
		finally {
			factoryScope.dropData();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testInterceptorStateful(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();
		var interceptor = new InterceptorImpl();

		try (var em = sf.createEntityManager( interceptor )) {
			em.inTransaction( (t) -> {
				assertThat( interceptor.getCounter() ).isEqualTo( 0 );
				em.persist( new TestEntity( 1, "Stuff" ) );
			} );
			assertThat( interceptor.getCounter() ).isEqualTo( 1 );
		}
		finally {
			factoryScope.dropData();
		}
	}

	@Test
	@DomainModel(annotatedClasses = EntityHandlerOptionTests.TestEntity.class)
	@SessionFactory
	void testInterceptorStateless(SessionFactoryScope factoryScope) {
		var sf = factoryScope.getSessionFactory();
		var interceptor = new InterceptorImpl();

		try (var em = sf.createEntityAgent( interceptor )) {
			em.inTransaction( (t) -> {
				assertThat( interceptor.getCounter() ).isEqualTo( 0 );
				em.insert( new TestEntity( 1, "Stuff" ) );
			} );
			assertThat( interceptor.getCounter() ).isEqualTo( 1 );
		}
		finally {
			factoryScope.dropData();
		}
	}

	@Entity
	public static class TestEntity {
		@Id
		private Integer id;
		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	public static class TestEntity2 {
		@Id
		private Integer id;
		private String name;
		@TenantId
		private String segment;
	}

	public static class InterceptorImpl implements Interceptor {
		private int counter;

		public int getCounter() {
			return counter;
		}

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			counter++;
			return false;
		}

		@Override
		public void onInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {
			counter++;
		}
	}
}
