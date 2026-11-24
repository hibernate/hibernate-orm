package org.hibernate.orm.test.collection.map;

import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.jdbc.spi.InsertSelectExecution;
import org.hibernate.generator.EventType;
import org.hibernate.orm.test.collection.map.LazyUniqueKeyAssociationTest.Catalog;
import org.hibernate.orm.test.collection.map.LazyUniqueKeyAssociationTest.Item;
import org.hibernate.orm.test.collection.map.LazyUniqueKeyAssociationTest.RegionalItem;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BytecodeEnhanced(runNotEnhancedAsWell = true)
@DomainModel(annotatedClasses = { Catalog.class, Item.class, RegionalItem.class,
		PropertyRefMutationTest.Stock.class, PropertyRefMutationTest.Reservation.class, PropertyRefMutationTest.Invoice.class })
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.FLUSH_QUEUE_TYPE, value = "graph"),
		@Setting(name = AvailableSettings.GRAPH_DEFER_IDENTITY_INSERTS, value = "true") },
		settingProviders = @SettingProvider(settingName = AvailableSettings.CONNECTION_PROVIDER,
				provider = PreparedStatementSpyConnectionProviderSettingProvider.class))
@SessionFactory(useCollectingStatementInspector = true)
class PropertyRefMutationTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 3; i++ ) {
				final var item = new Item();
				item.id = i;
				item.code = "KEY" + i;
				session.persist( item );
			}
			session.persist( new Catalog() );
		} );
	}

	@Test
	void updateWithReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update Catalog set featured_code='KEY1' where code=1" ).executeUpdate() );
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var catalog = session.find( Catalog.class, 1 );
			final var item = session.getReference( Item.class, 2 );
			sql.clear();
			catalog.featuredItem = item;
			session.flush();
			sql.assertExecutedCount( 1 );
			assertThat( sql.getLastQuery() ).containsIgnoringCase( "update Catalog" ).containsIgnoringCase( "exists" );
			assertThat( Hibernate.isInitialized( item ) ).isFalse();
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select featured_code from Catalog where code=1", String.class ).getSingleResult() ).isEqualTo( "KEY2" ) );
	}

	@Test
	void insertRowsWithLoadedAndUnfetchedKeys(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 8 );
			final var catalog = new Catalog();
			catalog.code = 2;
			final var loaded = session.find( Item.class, 1 );
			Hibernate.initializeProperty( loaded, "code" );
			final var second = session.getReference( Item.class, 2 );
			final var third = session.getReference( Item.class, 3 );
			catalog.labels = new LinkedHashMap<>();
			catalog.labels.put( loaded, "one" );
			catalog.labels.put( second, "two" );
			catalog.labels.put( third, "three" );
			catalog.featuredItem = second;
			sql.clear();
			session.persist( catalog );
			session.flush();
			if ( supportsInsertSelectBatch( scope ) ) {
				assertThat( sql.getSqlQueries() ).allSatisfy( statement ->
						assertThat( statement ).startsWithIgnoringCase( "insert" ) );
				// The rows are batched by statement: one batch binds the loaded key, one reads the others
				assertThat( sql.getSqlQueries() ).filteredOn( statement -> statement.contains( "catalog_labels" ) )
						.hasSize( 2 )
						.anySatisfy( statement -> assertThat( statement ).containsIgnoringCase( "values" ) )
						.anySatisfy( statement -> assertThat( statement ).containsIgnoringCase( " from Item " ) );
				assertThat( Hibernate.isInitialized( second ) ).isFalse();
				assertThat( Hibernate.isInitialized( third ) ).isFalse();
			}
			else {
				// A batched insert cannot read its row from a select, so the keys are fetched
				assertThat( sql.getSqlQueries() ).filteredOn( statement -> statement.startsWith( "insert" ) )
						.allSatisfy( statement -> assertThat( statement ).doesNotContain( " from Item " ) );
			}
		} );
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select featured_code from Catalog where code=2", String.class )
					.getSingleResult() ).isEqualTo( "KEY2" );
			assertThat( session.createNativeQuery( "select key_code from catalog_labels order by labels", String.class )
					.getResultList() ).containsExactly( "KEY1", "KEY3", "KEY2" );
		} );
	}

	@Test
	void interleavedRowsAreBatchedByStatement(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 8 );
			final var loaded = session.find( Item.class, 1 );
			Hibernate.initializeProperty( loaded, "code" );
			final var unfetched = session.getReference( Item.class, 2 );
			sql.clear();
			for ( int code = 2; code <= 5; code++ ) {
				final var catalog = new Catalog();
				catalog.code = code;
				catalog.featuredItem = code % 2 == 0 ? loaded : unfetched;
				session.persist( catalog );
			}
			session.flush();
			if ( supportsInsertSelectBatch( scope ) ) {
				// One batch binds the loaded key and one reads the other, though the rows alternate
				assertThat( sql.getSqlQueries() ).hasSize( 2 )
						.anySatisfy( statement -> assertThat( statement ).containsIgnoringCase( "values" ) )
						.anySatisfy( statement -> assertThat( statement ).containsIgnoringCase( " from Item " ) );
				assertThat( Hibernate.isInitialized( unfetched ) ).isFalse();
			}
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select featured_code from Catalog where code>1 order by code", String.class ).getResultList() )
				.containsExactly( "KEY1", "KEY2", "KEY1", "KEY2" ) );
	}

	@Test
	void mergeWithManagedTarget(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 2 );
			final boolean codeLoaded = Hibernate.isPropertyInitialized( item, "code" );
			final var catalog = new Catalog();
			catalog.code = 2;
			catalog.featuredItem = item;
			sql.clear();
			session.merge( catalog );
			session.flush();
			assertThat( sql.getSqlQueries() ).noneMatch( statement -> statement.startsWith( "select" )
					&& statement.contains( " from Item " ) );
			assertThat( sql.getLastQuery().contains( " from Item " ) ).isEqualTo( !codeLoaded );
			assertThat( Hibernate.isPropertyInitialized( item, "code" ) ).isEqualTo( codeLoaded );
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select featured_code from Catalog where code=2", String.class ).getSingleResult() ).isEqualTo( "KEY2" ) );
	}

	@Test
	void updateCollectionRow(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 0 );
			final var catalog = new Catalog();
			catalog.code = 2;
			final var item = session.getReference( Item.class, 3 );
			catalog.labels.put( item, "before" );
			session.persist( catalog );
			session.flush();
			assertThat( Hibernate.isInitialized( item ) ).isFalse();

			sql.clear();
			catalog.labels.put( item, "after" );
			session.flush();
			sql.assertExecutedCount( 1 );
			assertThat( sql.getLastQuery() ).startsWithIgnoringCase( "update catalog_labels" )
					.containsIgnoringCase( "key_code=(select" );
			assertThat( Hibernate.isInitialized( item ) ).isFalse();
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select labels from catalog_labels where key_code='KEY3'", String.class ).getSingleResult() )
				.isEqualTo( "after" ) );
	}

	@Test
	void missingOwnerWithExistingTarget(SessionFactoryScope scope) {
		assertThrows( OptimisticLockException.class, () -> scope.inTransaction( session -> {
			session.setJdbcBatchSize( 8 );
			final var catalog = session.find( Catalog.class, 1 );
			session.createNativeMutationQuery( "delete from Catalog where code=1" ).executeUpdate();
			catalog.featuredItem = session.getReference( Item.class, 2 );
			session.flush();
		} ) );
	}

	@Entity(name = "Invoice")
	static class Invoice {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		@ManyToOne
		@JoinColumn(name = "item_code", referencedColumnName = "code")
		Item item;

		@ManyToOne
		@JoinColumn(name = "other_item_code", referencedColumnName = "code")
		Item otherItem;

		@Generated(event = EventType.UPDATE, sql = "42")
		Integer revisionMarker;
	}

	@Test
	void staleUpdateWithGeneratedValue(SessionFactoryScope scope) {
		final Long id = scope.fromTransaction( session -> {
			final var invoice = new Invoice();
			session.persist( invoice );
			session.flush();
			return invoice.id;
		} );
		scope.inTransaction( session -> {
			final var invoice = session.find( Invoice.class, id );
			invoice.item = session.getReference( Item.class, 1 );
			session.flush();
			assertThat( invoice.revisionMarker ).isEqualTo( 42 );
		} );
		assertThrows( EntityNotFoundException.class, () -> scope.inTransaction( session -> {
			final var invoice = session.find( Invoice.class, id );
			invoice.item = session.getReference( Item.class, 99 );
			session.flush();
		} ) );
		assertThrows( OptimisticLockException.class, () -> scope.inTransaction( session -> {
			final var invoice = session.find( Invoice.class, id );
			session.createNativeMutationQuery( "delete from Invoice where id=:id" ).setParameter( "id", id )
					.executeUpdate();
			invoice.item = session.getReference( Item.class, 2 );
			session.flush();
		} ) );
	}

	@Test
	void insertWithDeferredIdentity(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		final Long invoiceId = scope.fromTransaction( session -> {
			final var invoice = new Invoice();
			invoice.item = session.getReference( Item.class, 2 );
			sql.clear();
			session.persist( invoice );
			session.flush();
			final boolean insertSelect = usesInsertSelectForInvoice( scope );
			assertThat( sql.getSqlQueries() ).anySatisfy( statement -> assertThat( statement )
					.containsIgnoringCase( "insert into Invoice" )
					.containsIgnoringCase( insertSelect ? " from Item " : "values" ) );
			assertThat( Hibernate.isInitialized( invoice.item ) ).isEqualTo( !insertSelect );
			return invoice.id;
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select item_code from Invoice where id=:id", String.class ).setParameter( "id", invoiceId )
				.getSingleResult() ).isEqualTo( "KEY2" ) );
		assertThrows( EntityNotFoundException.class, () -> scope.inTransaction( session -> {
			final var invoice = new Invoice();
			invoice.item = session.getReference( Item.class, 99 );
			session.persist( invoice );
			session.flush();
		} ) );
	}

	@Test
	void insertWithTwoReferencesToSameTable(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var invoice = new Invoice();
			invoice.item = session.getReference( Item.class, 1 );
			invoice.otherItem = session.getReference( Item.class, 2 );
			sql.clear();
			session.persist( invoice );
			session.flush();
			final boolean insertSelect = usesInsertSelectForInvoice( scope );
			if ( insertSelect ) {
				assertThat( sql.getSqlQueries() ).anySatisfy( statement -> assertThat( statement )
						.contains( "Item uk0", "Item uk1" ).doesNotContain( "exists" ) );
			}
			assertThat( Hibernate.isInitialized( invoice.item ) ).isEqualTo( !insertSelect );
			assertThat( Hibernate.isInitialized( invoice.otherItem ) ).isEqualTo( !insertSelect );
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select item_code, other_item_code from Invoice", Object[].class ).getSingleResult() )
				.containsExactly( "KEY1", "KEY2" ) );

		final var failure = assertThrows( EntityNotFoundException.class, () -> scope.inTransaction( session -> {
			final var invoice = new Invoice();
			invoice.item = session.getReference( Item.class, 1 );
			invoice.otherItem = session.getReference( Item.class, 99 );
			session.persist( invoice );
			session.flush();
		} ) );
		assertThat( failure ).hasMessageContaining( "99" );
	}

	private static boolean usesInsertSelectForInvoice(SessionFactoryScope scope) {
		return scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Invoice.class )
				.getInsertDelegate().supportsInsertSelect();
	}

	private static boolean supportsInsertSelectBatch(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getDialect().supports( InsertSelectExecution.BATCH );
	}

	@Embeddable
	record StockId(String warehouse, int binNumber) {}

	@Entity(name = "Stock")
	static class Stock {
		@EmbeddedId
		StockId id;
		String sku;
		String region;
	}

	@Entity(name = "Reservation")
	static class Reservation {
		@Id
		int id;

		@ManyToOne
		@JoinColumn(name = "stock_sku", referencedColumnName = "sku")
		@JoinColumn(name = "stock_region", referencedColumnName = "region")
		Stock stock;
	}

	@Test
	void compositeKeys(SessionFactoryScope scope) {
		final var id = new StockId( "Dublin", 3 );
		scope.inTransaction( session -> {
			final var stock = new Stock();
			stock.id = id;
			stock.sku = "S42";
			stock.region = "IE";
			session.persist( stock );
		} );
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var stock = session.getReference( Stock.class, id );
			final var reservation = new Reservation();
			reservation.id = 1;
			reservation.stock = stock;
			sql.clear();
			session.persist( reservation );
			session.flush();
			sql.assertExecutedCount( 1 );
			assertThat( sql.getLastQuery() ).containsOnlyOnce( " from Stock " )
					.contains( "uk0.sku", "uk0.region" ).doesNotContain( "exists" );
			// One reservation id and one two-column stock id.
			assertThat( sql.getLastQuery().chars().filter( ch -> ch == '?' ).count() ).isEqualTo( 3 );
			assertThat( Hibernate.isInitialized( stock ) ).isFalse();
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select stock_sku, stock_region from Reservation", Object[].class ).getSingleResult() )
				.containsExactly( "S42", "IE" ) );
	}

	@Test
	void updateWithCompositeTargetIdentifier(SessionFactoryScope scope) {
		final var id = new StockId( "Dublin", 3 );
		scope.inTransaction( session -> {
			final var stock = new Stock();
			stock.id = id;
			stock.sku = "S42";
			stock.region = "IE";
			session.persist( stock );
			final var reservation = new Reservation();
			reservation.id = 1;
			session.persist( reservation );
		} );
		scope.inTransaction( session -> {
			final var reservation = session.find( Reservation.class, 1 );
			reservation.stock = session.getReference( Stock.class, id );
			final var sql = scope.getCollectingStatementInspector();
			sql.clear();
			session.flush();
			sql.assertExecutedCount( 1 );
			assertThat( sql.getLastQuery() ).contains( "exists", "uk.sku", "uk.region" );
			assertThat( Hibernate.isInitialized( reservation.stock ) ).isFalse();
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select stock_sku, stock_region from Reservation", Object[].class ).getSingleResult() )
				.containsExactly( "S42", "IE" ) );
	}

	@Test
	void partiallyFetchedCompositeMapKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new RegionalItem();
			item.code = "D42";
			item.region = "EU";
			session.persist( item );
		} );
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var catalog = new Catalog();
			catalog.code = 2;
			final var item = session.find( RegionalItem.class, 1 );
			final boolean codeLoaded = Hibernate.isPropertyInitialized( item, "code" );
			catalog.regionalLabels.put( item, "member" );
			sql.clear();
			session.persist( catalog );
			session.flush();
			assertThat( sql.getSqlQueries() ).allSatisfy( statement ->
					assertThat( statement ).startsWithIgnoringCase( "insert" ) );
			assertThat( sql.getLastQuery().contains( "uk0.code" ) ).isEqualTo( !codeLoaded );
			assertThat( sql.getLastQuery() ).doesNotContainIgnoringCase( "uk0.region" );
			assertThat( Hibernate.isPropertyInitialized( item, "code" ) ).isEqualTo( codeLoaded );
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select key_code, key_region from Catalog_regionalLabels", Object[].class ).getSingleResult() )
				.containsExactly( "D42", "EU" ) );
	}

	@Test
	void missingMapKeyWithUnknownBatchCounts(SessionFactoryScope scope) {
		final var provider = (PreparedStatementSpyConnectionProvider) scope.getSessionFactory()
				.getProperties().get( AvailableSettings.CONNECTION_PROVIDER );
		final var masked = new AtomicInteger();
		provider.spyContext.registerCallback( (statement, method, args, result) -> {
			if ( masked.get() == 0 && method.getName().equals( "executeBatch" )
					&& provider.getPreparedStatementsAndSql().get( statement ).contains( "catalog_labels" ) ) {
				final var counts = (int[]) result;
				Arrays.fill( counts, Statement.SUCCESS_NO_INFO );
				masked.addAndGet( counts.length );
			}
		} );
		final var failure = assertThrows( EntityNotFoundException.class, () -> scope.inTransaction( session -> {
			session.setJdbcBatchSize( 8 );
			final var catalog = new Catalog();
			catalog.code = 2;
			catalog.labels = new LinkedHashMap<>();
			catalog.labels.put( session.getReference( Item.class, 2 ), "present" );
			catalog.labels.put( session.getReference( Item.class, 99 ), "missing" );
			session.persist( catalog );
			session.flush();
		} ) );
		// Without a statement which reads the key, binding the missing key fails before the batch executes
		assertThat( masked.get() ).isEqualTo( supportsInsertSelectBatch( scope ) ? 2 : 0 );
		assertThat( failure ).hasMessageContaining( "99" );
		scope.inSession( session -> assertThat( session.find( Catalog.class, 2 ) ).isNull() );
	}

	@Test
	void missingMapKey(SessionFactoryScope scope) {
		assertThrows( EntityNotFoundException.class, () -> scope.inTransaction( session -> {
			session.setJdbcBatchSize( 8 );
			final var catalog = new Catalog();
			catalog.code = 2;
			catalog.labels.put( session.getReference( Item.class, 99 ), "missing" );
			session.persist( catalog );
			session.flush();
		} ) );
		scope.inSession( session -> assertThat( session.find( Catalog.class, 2 ) ).isNull() );
	}

	@Test
	void missingUpdateTarget(SessionFactoryScope scope) {
		assertThrows( EntityNotFoundException.class, () -> scope.inTransaction( session -> {
			session.setJdbcBatchSize( 0 );
			final var catalog = session.find( Catalog.class, 1 );
			catalog.featuredItem = session.getReference( Item.class, 99 );
			session.flush();
		} ) );
	}
}
