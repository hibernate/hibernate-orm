package org.hibernate.orm.test.collection.map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.orm.test.collection.map.PropertyRefMutationTest.Reservation;
import org.hibernate.orm.test.collection.map.PropertyRefMutationTest.Stock;
import org.hibernate.orm.test.collection.map.PropertyRefMutationTest.StockId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(H2Dialect.class)
class PropertyRefInsertDialectTest {
	@Test
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.orm.test.collection.map.PropertyRefInsertDialectTest$RestrictedSourceDialect"),
			@Setting(name = AvailableSettings.FLUSH_QUEUE_TYPE, value = "graph")
	})
	@DomainModel(annotatedClasses = { Stock.class, Reservation.class })
	@SessionFactory(useCollectingStatementInspector = true)
	void insertWithRestrictedSingleRowSource(SessionFactoryScope scope) {
		final var sql = insertReservation( scope );
		assertThat( sql ).containsIgnoringCase( "where x=1" ).containsIgnoringCase( "exists" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.orm.test.collection.map.PropertyRefInsertDialectTest$NoSelectListSubqueryDialect"),
			@Setting(name = AvailableSettings.FLUSH_QUEUE_TYPE, value = "graph")
	})
	@DomainModel(annotatedClasses = { Stock.class, Reservation.class })
	@SessionFactory(useCollectingStatementInspector = true)
	void fetchKeyWhenSelectListSubqueriesAreUnsupported(SessionFactoryScope scope) {
		assertThat( insertReservation( scope ) ).containsIgnoringCase( "values" );
	}

	private String insertReservation(SessionFactoryScope scope) {
		final var id = new StockId( "Dublin", 3 );
		scope.inTransaction( session -> {
			final var stock = new Stock();
			stock.id = id;
			stock.sku = "S42";
			stock.region = "IE";
			session.persist( stock );
		} );
		scope.inTransaction( session -> {
			final var reservation = new Reservation();
			reservation.id = 1;
			reservation.stock = session.getReference( Stock.class, id );
			session.persist( reservation );
		} );
		final String insert = scope.getCollectingStatementInspector().getLastQuery();
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select stock_sku, stock_region from Reservation", Object[].class ).getSingleResult() )
				.containsExactly( "S42", "IE" ) );
		return insert;
	}

	public static class RestrictedSourceDialect extends H2Dialect {
		@Override
		public SingleRowTableSupport getSingleRowTableSupport() {
			return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
					.selectOnlyFromClause( " from (values (1),(2)) d(x) where x=1" )
					.build();
		}
	}

	public static class NoSelectListSubqueryDialect extends H2Dialect {
		@Override
		public SubquerySupport getSubquerySupport() {
			return SubquerySupport.builder( super.getSubquerySupport() )
					.feature( SubquerySupport.Feature.SELECT_LIST, false )
					.build();
		}
	}
}
