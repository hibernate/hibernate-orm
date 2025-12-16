/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cache.internal.NaturalIdCacheKey;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.engine.internal.NaturalIdResolutionsImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.KeyType.NATURAL;

/// Tests for [org.hibernate.engine.spi.NaturalIdResolutions]
///
/// @author Steve Ebersole
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting( name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
@DomainModel(annotatedClasses = {XRefTests.Book.class,XRefTests.Bookmark.class,XRefTests.Pen.class})
@SessionFactory(useCollectingStatementInspector = true)
public class XRefTests {
	public static final String BOOK_ISBN = "123-4567-890";
	public static final String BOOKMARK_SKU = "98-abc-7654-def";
	public static final String PEN_SKU = "98-xyz-7234";

	@Test
	void testLocalResolution(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			final NaturalIdResolutionsImpl naturalIdResolutions = (NaturalIdResolutionsImpl) session.getPersistenceContext().getNaturalIdResolutions();
			assertThat( naturalIdResolutions.getEntityResolutions( Book.class ) ).isNull();

			session.find( XRefTests.Book.class, BOOK_ISBN, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			verifyLocalResolution( Book.class, BOOK_ISBN, 1, session );

			sqlCollector.clear();

			session.find( XRefTests.Book.class, BOOK_ISBN, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
			verifyLocalResolution( Book.class, BOOK_ISBN, 1, session );
		} );
	}

	@Test
	void testLocalResolutionWithCache(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.getSessionFactory().getCache().evictAllRegions();

		factoryScope.inTransaction( (session) -> {
			verifyEmptyLocalResolution( Bookmark.class, session );
			verifyEmptyCacheResolution( Bookmark.class, BOOKMARK_SKU, session );

			session.find( Bookmark.class, BOOKMARK_SKU, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			verifyLocalResolution( Bookmark.class, BOOKMARK_SKU, 1, session );
			verifyCacheResolution( Bookmark.class, BOOKMARK_SKU, 1, session );

			sqlCollector.clear();

			session.find( Bookmark.class, BOOKMARK_SKU, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
			verifyLocalResolution( Bookmark.class, BOOKMARK_SKU, 1, session );
			verifyCacheResolution( Bookmark.class, BOOKMARK_SKU, 1, session );
		} );

		factoryScope.inTransaction( (session) -> {
			verifyCacheResolution( Bookmark.class, BOOKMARK_SKU, 1, session );
			verifyEmptyLocalResolution( Bookmark.class, session );
		} );
	}

	@Test
	void testLocalResolutionWithMutableCache(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.getSessionFactory().getCache().evictAllRegions();

		factoryScope.inTransaction( (session) -> {
			verifyEmptyLocalResolution( Pen.class, session );
			verifyEmptyCacheResolution( Pen.class, PEN_SKU, session );

			session.find( XRefTests.Pen.class, PEN_SKU, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			verifyLocalResolution( Pen.class, PEN_SKU, 1, session );
			verifyCacheResolution( Pen.class, PEN_SKU, 1, session );

			sqlCollector.clear();

			session.find( XRefTests.Pen.class, PEN_SKU, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
			verifyLocalResolution( Pen.class, PEN_SKU, 1, session );
			verifyCacheResolution( Pen.class, PEN_SKU, 1, session );
		} );

		factoryScope.inTransaction( (session) -> {
			verifyCacheResolution( Pen.class, PEN_SKU, 1, session );
			verifyEmptyLocalResolution( Pen.class, session );
		} );
	}

	@Test
	void testCrossRefManagementWithMutation(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		final String updatedSku = "987-123-654";
		factoryScope.inTransaction( (session) -> {
			verifyCacheResolution( Pen.class, PEN_SKU, 1, session );
			verifyEmptyLocalResolution( Pen.class, session );

			session.find( Pen.class, PEN_SKU, NATURAL );
			verifyLocalResolution( Pen.class, PEN_SKU, 1, session );

			factoryScope.inTransaction( (session2) -> {
				session2.find( Pen.class, 1 ).sku = updatedSku;
			} );

			// the point here -
			//		1. The local (Session) cache maintains repeatable-read
			//		2. The shared (L2C) cache sees the updated xref
			verifyLocalResolution( Pen.class, PEN_SKU, 1, session );
			verifyCacheResolution( Pen.class, updatedSku, 1, session );

			sqlCollector.clear();

			session.find( Pen.class, PEN_SKU, NATURAL );
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
			verifyLocalResolution( Pen.class, PEN_SKU, 1, session );
			verifyCacheResolution( Pen.class, updatedSku, 1, session );
		} );

		factoryScope.inTransaction( (session) -> {
			var nonExistent = session.find( Pen.class, PEN_SKU, NATURAL );
			assertThat( nonExistent ).isNull();

			var updated = session.find( Pen.class, updatedSku, NATURAL );
			assertThat( updated ).isNotNull();

			verifyLocalResolution( Pen.class, updatedSku, 1, session );
			verifyCacheResolution( Pen.class, updatedSku, 1, session );
		} );
	}

	private void verifyEmptyLocalResolution(Class<?> entityType, SessionImplementor session) {
		final NaturalIdResolutionsImpl naturalIdResolutions = (NaturalIdResolutionsImpl) session.getPersistenceContext().getNaturalIdResolutions();
		assertThat( naturalIdResolutions.getEntityResolutions( entityType ) ).isNull();
	}

	private void verifyLocalResolution(Class<?> entityType, String naturalId, int id, SessionImplementor session) {
		final NaturalIdResolutionsImpl naturalIdResolutions = (NaturalIdResolutionsImpl) session.getPersistenceContext().getNaturalIdResolutions();

		var entityResolutions = naturalIdResolutions.getEntityResolutions( entityType );
		assertThat( entityResolutions ).isNotNull();
		assertThat( entityResolutions.getResolutionByPk( id ).getNaturalIdValue() ).isEqualTo( naturalId );
		assertThat( entityResolutions.getIdResolutionByNaturalId( naturalId ) ).isEqualTo( id );
	}

	private void verifyEmptyCacheResolution(Class<?> entityType, Object naturalId, SessionImplementor session) {
		final CacheImplementor cache = session.getSessionFactory().getCache();
		final DomainDataRegionTemplate region = (DomainDataRegionTemplate) cache.getRegion( entityType.getName() + "##NaturalId" );
		// region should get created on bootstrap
		assertThat( region ).isNotNull();
		// however, we should have no cached resolutions
		assertThat( region.getCacheStorageAccess().contains( naturalId ) ).isFalse();
	}

	private void verifyCacheResolution(Class<?> entityType, String naturalId, int id, SessionImplementor session) {
		final CacheImplementor cache = session.getSessionFactory().getCache();
		final DomainDataRegionTemplate region = (DomainDataRegionTemplate) cache.getRegion( entityType.getName() + "##NaturalId" );
		var storage = region.getCacheStorageAccess();
		var entityDescriptor = session.getFactory().getMappingMetamodel().getEntityDescriptor( entityType.getName() );
		var resolutionKey = NaturalIdCacheKey.from( naturalId, entityDescriptor, entityDescriptor.getEntityName(), session );
		var resolution = (AbstractReadWriteAccess.Item) storage.getFromCache( resolutionKey, session );
		assertThat( resolution.getValue() ).isEqualTo( id );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Book( 1, "The Grapes of Wrath", BOOK_ISBN ) );
			session.persist( new Bookmark( 1, "The Keeper", BOOKMARK_SKU ) );
			session.persist( new Pen( 1, "G2", PEN_SKU ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity(name="Book")
	@Table(name="books")
	public static class Book {
		@Id
		private Integer id;
		private String title;
		@NaturalId
		private String isbn;

		public Book() {
		}

		public Book(Integer id, String title, String isbn) {
			this.id = id;
			this.title = title;
			this.isbn = isbn;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity(name="Bookmark")
	@Table(name="bookmarks")
	@NaturalIdCache
	public static class Bookmark {
		@Id
		private Integer id;
		private String manufacturer;
		@NaturalId
		private String sku;

		public Bookmark() {
		}

		public Bookmark(Integer id, String manufacturer, String sku) {
			this.id = id;
			this.manufacturer = manufacturer;
			this.sku = sku;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity(name="Pen")
	@Table(name="pens")
	@NaturalIdCache
	public static class Pen {
		@Id
		private Integer id;
		private String manufacturer;
		@NaturalId(mutable = true)
		private String sku;

		public Pen() {
		}

		public Pen(Integer id, String manufacturer, String sku) {
			this.id = id;
			this.manufacturer = manufacturer;
			this.sku = sku;
		}
	}
}
