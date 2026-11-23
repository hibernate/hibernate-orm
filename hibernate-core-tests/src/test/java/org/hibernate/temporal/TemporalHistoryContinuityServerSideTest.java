package org.hibernate.temporal;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Every history row written by one transaction must carry the same server
/// timestamp, so that a superseded row ends exactly where its successor starts.
/// Databases whose current timestamp changes between the statements of a
/// transaction would otherwise leave gaps in the history.
///
/// @author Steve Ebersole
@SessionFactory
@DomainModel(annotatedClasses = TemporalHistoryContinuityServerSideTest.ContinuityRow.class)
@ServiceRegistry(settings = {@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = StateManagementSettings.USE_SERVER_TRANSACTION_TIMESTAMPS, value = "true")})
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 11, versionMatchMode = VersionMatchMode.OLDER, reason = "See https://jira.mariadb.org/browse/MDEV-39230")
class TemporalHistoryContinuityServerSideTest {
	@Test
	void oneTransactionWritesOneTimestamp(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				final var row = new ContinuityRow();
				row.id = 1L;
				row.text = "first";
				row.strings.add( "x" );
				session.persist( row );
			} );
			scope.inTransaction( session -> {
				final var row = session.find( ContinuityRow.class, 1L );
				row.text = "second";
				row.strings.remove( "x" );
				row.strings.add( "y" );
			} );
			scope.inTransaction( session -> session.doWork( connection -> {
				final Timestamp entityEnd = single( connection,
						"select effective_to from ContinuityRow_history where id=1 and text='first'" );
				final Timestamp entityStart = single( connection,
						"select effective_from from ContinuityRow_history where id=1 and text='second'" );
				final Timestamp elementEnd = single( connection,
						"select superseded from ContinuityRow_strings_history where strings='x'" );
				final Timestamp elementStart = single( connection,
						"select effective from ContinuityRow_strings_history where strings='y'" );
				assertEquals( entityEnd, entityStart, "entity history is contiguous" );
				assertEquals( elementEnd, elementStart, "collection history is contiguous" );
				assertEquals( entityStart, elementStart, "entity and collection history share the transaction timestamp" );
			} ) );
		}
		finally {
			scope.dropData();
		}
	}

	private static Timestamp single(Connection connection, String sql) throws SQLException {
		try ( var statement = connection.createStatement(); var results = statement.executeQuery( sql ) ) {
			results.next();
			final Timestamp value = results.getTimestamp( 1 );
			assertNotNull( value, sql );
			return value;
		}
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "ContinuityRow")
	static class ContinuityRow {
		@Id
		long id;
		String text;
		@Temporal
		@ElementCollection
		Set<String> strings = new HashSet<>();
	}
}
