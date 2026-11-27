package org.hibernate.temporal;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.orm.test.secondarytable.OptionalSecondaryRowFallbackTest$FallbackDialect"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
class TemporalGeneratedUpsertDefaultHistoryFallbackTest extends TemporalGeneratedUpsertDefaultHistoryTest {
	@Test
	void reuseInsertPositionsWithDifferentRows(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			for ( int id = 2; id <= 3; id++ ) {
				var item = new Item();
				item.id = id;
				item.answer = 99;
				item.quantity = id * 10;
				session.upsert( item );
			}
		} );
		scope.inSession( session -> {
			for ( int id = 2; id <= 3; id++ ) {
				var item = session.find( Item.class, id );
				assertEquals( 42, item.answer );
				assertEquals( id * 10, item.quantity );
			}
		} );
	}
}
