package org.hibernate.temporal;

import java.util.Locale;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@JiraKey("HHH-21291")
@DomainModel(annotatedClasses = TemporalExcludedHistoryVersionTest.Item.class)
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph")
})
class TemporalExcludedHistoryVersionTest {
	@Entity(name = "ExcludedItem")
	@Temporal
	static class Item {
		@Id int id = 1;
		@Version int version;
		@Temporal.Excluded String note;
	}

	@Test
	void openRevisionKeepsItsVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item() ) );
		var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> session.find( Item.class, 1 ).note = "edited" );
		// the open revision is the one which has not ended, whatever its version
		observer.assertQueries().map( sql -> sql.toLowerCase( Locale.ROOT ) )
				.filteredOn( sql -> sql.startsWith( "update " ) && sql.contains( "excludeditem_history" ) )
				.singleElement().asString().doesNotContain( "version" ).contains( "superseded is null" );
		// the version of the current row is now two ahead of the version of its revision
		scope.inTransaction( session -> session.find( Item.class, 1 ).note = "edited again" );
		scope.inSession( session -> {
			var revision = session.createNativeQuery(
					"select version, note from ExcludedItem_history", Object[].class ).getSingleResult();
			assertThat( ( (Number) revision[0] ).intValue() ).isZero();
			assertThat( revision[1] ).isEqualTo( "edited again" );
		} );
	}
}
