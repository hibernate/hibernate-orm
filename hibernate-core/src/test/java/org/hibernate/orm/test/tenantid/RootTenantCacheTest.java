package org.hibernate.orm.test.tenantid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Column;

import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = { RootTenantCacheTest.Item.class, RootTenantCacheTest.EmbeddedItem.class, RootTenantCacheTest.RestrictedItem.class, RootTenantCacheTest.Owner.class })
@SessionFactory(generateStatistics = true, useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER, value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false"),
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = USE_QUERY_CACHE, value = "true"),
		@Setting(name = CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory")
})
class RootTenantCacheTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	enum Mutation {
		UPDATE, DELETE, COLLECTION, STATELESS_UPDATE, STATELESS_DELETE, UPSERT
	}

	private Item prime(SessionFactoryScope scope) {
		final var item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		inTenant( scope, "mine", session -> {
			final var loaded = session.bySimpleNaturalId( Item.class ).load( "before" );
			assertEquals( List.of( "before" ), loaded.values );
		} );
		return item;
	}

	@Test
	void ordinaryFilterStillBypassesTenantCache(SessionFactoryScope scope) {
		prime( scope );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		inTenant( scope, "mine", session -> {
			final var filter = session.enableFilter( "visibleRootCacheItem" ).setParameter( "code", "hidden" );
			assertNull( session.find( Item.class, 1L ) );
			assertNotNull( session.getEnabledFilter( "_tenantId" ) );
			filter.setParameter( "code", "before" );
			assertNotNull( session.find( Item.class, 1L ) );
		} );
		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
	}

	@Test
	void tenantFilterStillHidesCachedAssociationTarget(SessionFactoryScope scope) {
		inTenant( scope, "yours", session -> session.persist( new Item() ) );
		inTenant( scope, "mine", session -> {
			final var owner = new Owner();
			owner.item = session.getReference( Item.class, 1L );
			session.persist( owner );
		} );
		inTenant( scope, "mine", session -> {
			final var owner = session.find( Owner.class, 1L );
			assertNotNull( owner );
			assertNull( owner.item );
			owner.name = "after";
		} );
		inTenant( scope, "root", session -> {
			final var owner = session.find( Owner.class, 1L );
			assertEquals( "after", owner.name );
			assertEquals( 1L, owner.item.id );
		} );
	}

	@Test
	void unrelatedFilterDoesNotBypassTenantCache(SessionFactoryScope scope) {
		prime( scope );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		inTenant( scope, "mine", session -> {
			session.enableFilter( "otherRootCacheItem" );
			assertNotNull( session.find( Item.class, 1L ) );
		} );
		assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
	}

	@Test
	void rootInvalidatesEmbeddedTenantWithProvidedLoader(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new EmbeddedItem() ) );
		inTenant( scope, "mine", session -> assertEquals( "before", session.find( EmbeddedItem.class, 1L ).name ) );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		inTenant( scope, "mine", session -> assertEquals( "before", session.find( EmbeddedItem.class, 1L ).name ) );
		assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
		inTenant( scope, "root", session -> session.find( EmbeddedItem.class, 1L ).name = "after" );
		inTenant( scope, "mine", session -> assertEquals( "after", session.find( EmbeddedItem.class, 1L ).name ) );
	}

	@Test
	void rootInvalidatesRowExcludedByStaticRestriction(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new RestrictedItem() ) );
		inTenant( scope, "mine", session -> session.find( RestrictedItem.class, 1L ).name = "hidden" );
		TenantIdMutationMappingTest.inStatelessTenant( scope, "root", session -> {
			final var item = new RestrictedItem();
			item.version = 1;
			item.name = "after";
			session.update( item );
		} );
		inTenant( scope, "mine", session -> assertEquals( "after", session.find( RestrictedItem.class, 1L ).name ) );
	}

	@Test
	void rootInvalidationReadsOnlyOwnerAndVersion(SessionFactoryScope scope) {
		prime( scope );
		inTenant( scope, "root", session -> {
			final var item = session.find( Item.class, 1L );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			item.code = "after";
			session.flush();
			final var selects = inspector.getSqlQueries().stream().filter( sql -> sql.startsWith( "select " ) ).toList();
			assertEquals( 1, selects.size() );
			final String projection = selects.get( 0 ).substring( 0, selects.get( 0 ).indexOf( " from " ) );
			assertTrue( projection.contains( "tenant" ) );
			assertTrue( projection.contains( "version" ) );
			assertFalse( projection.contains( "code" ) );
		} );
		inTenant( scope, "mine", session -> assertEquals( "after", session.find( Item.class, 1L ).code ) );
	}

	@ParameterizedTest
	@EnumSource(Mutation.class)
	void rootWriteInvalidatesOwner(Mutation mutation, SessionFactoryScope scope) {
		final Item item = prime( scope );
		if ( mutation.name().startsWith( "STATELESS" ) || mutation == Mutation.UPSERT ) {
			item.tenant = "yours"; // the stored owner, not detached state, determines the cache key
			item.code = "after";
			item.values = new ArrayList<>( List.of( "after" ) );
			try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( "root" ).openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				switch ( mutation ) {
					case STATELESS_UPDATE -> session.update( item );
					case STATELESS_DELETE -> session.delete( item );
					case UPSERT -> session.upsert( item );
					default -> throw new AssertionError();
				}
				transaction.commit();
			}
		}
		else {
			inTenant( scope, "root", session -> {
				final var loaded = session.find( Item.class, 1L );
				if ( mutation == Mutation.DELETE ) {
					session.remove( loaded );
				}
				else {
					loaded.values.clear();
					loaded.values.add( "after" );
					if ( mutation == Mutation.UPDATE ) {
						loaded.code = "after";
					}
				}
			} );
		}
		inTenant( scope, "mine", session -> {
			final var loaded = session.find( Item.class, 1L );
			if ( mutation == Mutation.DELETE || mutation == Mutation.STATELESS_DELETE ) {
				assertNull( loaded );
				assertNull( session.bySimpleNaturalId( Item.class ).getReference( "before" ) );
			}
			else {
				assertEquals( mutation == Mutation.COLLECTION ? "before" : "after", loaded.code );
				assertEquals( List.of( "after" ), loaded.values );
				if ( mutation != Mutation.COLLECTION ) {
					assertNull( session.bySimpleNaturalId( Item.class ).getReference( "before" ) );
				}
			}
		} );
	}

	@Test
	void rootDoesNotReadOrPopulateCaches(SessionFactoryScope scope) {
		prime( scope );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		for ( int i = 0; i < 2; i++ ) {
			inTenant( scope, "root", session -> {
				session.setCacheMode( CacheMode.NORMAL );
				final var graph = session.createEntityGraph( Item.class );
				graph.addAttributeNode( "values" ).addOption( CacheStoreMode.USE );
				session.createSelectionQuery( "from RootCacheItem", Item.class )
						.setEntityGraph( graph, org.hibernate.graph.GraphSemantic.FETCH ).getResultList();
				assertEquals( List.of( "before" ), session.find( Item.class, 1L ).values );
				assertEquals( 1L, session.getIdentifier( session.bySimpleNaturalId( Item.class ).getReference( "before" ) ) );
				session.createSelectionQuery( "from RootCacheItem", Item.class )
						.setCacheable( true ).setCacheMode( CacheMode.NORMAL ).getResultList();
			} );
		}
		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
		assertEquals( 0, statistics.getNaturalIdCacheHitCount() );
		assertEquals( 0, statistics.getNaturalIdCachePutCount() );
		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 0, statistics.getQueryCachePutCount() );
		inTenant( scope, "mine", session -> assertNotNull( session.find( Item.class, 1L ) ) );
		assertTrue( statistics.getSecondLevelCacheHitCount() > 0 );
	}

	@Test
	void cacheLocksSurviveFlushUntilRollback(SessionFactoryScope scope) {
		prime( scope );
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class );
		final var cache = persister.getCacheAccessStrategy();
		final var key = cache.generateCacheKey( 1L, persister, scope.getSessionFactory(), "mine" );
		try ( var reader = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession();
				var root = scope.getSessionFactory().withOptions().tenantIdentifier( "root" ).openSession() ) {
			final Object oldEntry = cache.get( reader, key );
			assertNotNull( oldEntry );
			final var transaction = root.beginTransaction();
			root.find( Item.class, 1L ).code = "after";
			root.flush();
			assertNull( cache.get( reader, key ) );
			assertFalse( cache.putFromLoad( reader, key, oldEntry, 0 ) );
			transaction.rollback();
			assertFalse( cache.putFromLoad( reader, key, oldEntry, 0 ) );
		}
		inTenant( scope, "mine", session -> assertEquals( "before", session.find( Item.class, 1L ).code ) );
		inTenant( scope, "mine", session -> assertNotNull( session.bySimpleNaturalId( Item.class ).load( "before" ) ) );
	}

	@Test
	void rootInsertDoesNotPopulateCaches(SessionFactoryScope scope) {
		final Item item = new Item();
		item.tenant = "mine";
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		inTenant( scope, "root", session -> session.persist( item ) );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
		assertEquals( 0, statistics.getNaturalIdCachePutCount() );
		inTenant( scope, "mine", session -> assertNotNull( session.bySimpleNaturalId( Item.class ).load( "before" ) ) );
	}

	@Test
	void existingRootCacheEntryIsIgnored(SessionFactoryScope scope) {
		prime( scope );
		final var factory = scope.getSessionFactory();
		final var persister = factory.getMappingMetamodel().getEntityDescriptor( Item.class );
		final var cache = persister.getCacheAccessStrategy();
		inTenant( scope, "mine", session -> {
			final Object entry = cache.get( session, cache.generateCacheKey( 1L, persister, factory, "mine" ) );
			assertNotNull( entry );
			assertTrue( cache.putFromLoad( session, cache.generateCacheKey( 1L, persister, factory, "root" ), entry, 0 ) );
			session.find( Item.class, 1L ).code = "after";
		} );
		inTenant( scope, "root", session -> assertEquals( "after", session.find( Item.class, 1L ).code ) );
	}

	@Test
	void rootUpdatePreservesOtherEntityEntries(SessionFactoryScope scope) {
		prime( scope );
		final var other = new Item();
		other.id = 2L;
		other.code = "other";
		inTenant( scope, "yours", session -> session.persist( other ) );
		inTenant( scope, "yours", session -> assertNotNull( session.find( Item.class, 2L ) ) );
		inTenant( scope, "root", session -> session.find( Item.class, 1L ).code = "after" );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		inTenant( scope, "yours", session -> assertEquals( "other", session.find( Item.class, 2L ).code ) );
		assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
	}

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "OPTIMISTIC_FORCE_INCREMENT", "PESSIMISTIC_FORCE_INCREMENT" })
	void rootVersionIncrementInvalidatesOwner(LockMode mode, SessionFactoryScope scope) {
		final Item item = prime( scope );
		inTenant( scope, "root", session -> session.lock( session.find( Item.class, 1L ), mode ) );
		inTenant( scope, "mine", session -> assertEquals( item.version + 1, session.find( Item.class, 1L ).version ) );
	}

	private static void inTenant(SessionFactoryScope scope, String tenant, Consumer<SessionImplementor> action) {
		scope.inTransaction( factory -> factory.withOptions().tenantIdentifier( tenant ).openSession(), action );
	}

	@Entity(name = "RestrictedRootCacheItem")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@SQLRestriction("name <> 'hidden'")
	static class RestrictedItem {
		@Id Long id = 1L;
		@Version int version;
		@TenantId String tenant;
		String name = "before";
	}

	@Entity(name = "EmbeddedRootCacheItem")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@HQLSelect(query = "from EmbeddedRootCacheItem where id=?1")
	@FilterDef(name = "otherRootCacheItem", applyToLoadByKey = true)
	@Filter(name = "otherRootCacheItem", condition = "1=1")
	static class EmbeddedItem {
		@Id Long id = 1L;
		@Version int version;
		@Embedded TenantDetails details = new TenantDetails();
		String name = "before";
	}

	@Embeddable
	static class TenantDetails {
		@TenantId String tenant;
	}

	@Entity(name = "RootCacheOwner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Owner {
		@Id Long id = 1L;
		@TenantId String tenant;
		String name = "before";
		@ManyToOne Item item;
	}

	@Entity(name = "RootCacheItem")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@NaturalIdCache
	@FilterDef(name = "visibleRootCacheItem", applyToLoadByKey = true,
			parameters = @ParamDef(name = "code", type = String.class))
	@Filter(name = "visibleRootCacheItem", condition = "code = :code")
	static class Item {
		@Id Long id = 1L;
		@Version Integer version;
		@TenantId String tenant;
		@NaturalId(mutable = true) String code = "before";
		@ElementCollection
		@Column(name = "entry_value")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		List<String> values = new ArrayList<>( List.of( "before" ) );
	}
}
