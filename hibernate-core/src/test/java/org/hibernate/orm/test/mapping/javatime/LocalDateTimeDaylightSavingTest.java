package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.assertj.core.api.Assertions.assertThat;

/// Direct JDBC handling preserves local date-time values inside daylight-saving gaps and overlaps.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = LocalDateTimeDaylightSavingTest.TimeEntry.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"/*the default anyway, but...*/))
@SessionFactory
@RequiresDialectFeature(feature = LocalDateTimeDaylightSavingTest.DirectLocalDateTimeSupport.class)
@SkipForDialect(dialectClass = AltibaseDialect.class,
		reason = "Altibase JDBC converts LocalDateTime through java.sql.Timestamp in the JVM default time zone, shifting values in daylight-saving gaps")
@JiraKey("HHH-16722")
@ResourceLock("java.util.TimeZone")
public class LocalDateTimeDaylightSavingTest {

	@Test
	void testLisbonDaylightSavingTransitions(SessionFactoryScope scope) {
		final ZoneId lisbon = ZoneId.of( "Europe/Lisbon" );
		final List<LocalDateTime> expected = List.of(
				LocalDateTime.parse( "2023-03-26T00:00" ),
				LocalDateTime.parse( "2023-03-26T01:00" ),
				LocalDateTime.parse( "2023-03-26T02:00" ),
				LocalDateTime.parse( "2023-03-26T03:00" ),
				LocalDateTime.parse( "2023-10-29T00:00" ),
				LocalDateTime.parse( "2023-10-29T01:00" ),
				LocalDateTime.parse( "2023-10-29T02:00" ),
				LocalDateTime.parse( "2023-10-29T03:00" )
		);
		assertThat( lisbon.getRules().getValidOffsets( expected.get( 1 ) ) ).isEmpty();
		assertThat( lisbon.getRules().getValidOffsets( expected.get( 5 ) ) ).hasSize( 2 );

		final TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault( TimeZone.getTimeZone( lisbon ) );
			assertThat( scope.getSessionFactory().getSessionFactoryOptions()
					.isDirectJavaTimeJdbcAccessEnabled( LocalDateTime.class ) ).isTrue();

			scope.inTransaction( session -> {
				for ( int i = 0; i < expected.size(); i++ ) {
					final var entry = new TimeEntry();
					entry.id = i;
					entry.localTime = expected.get( i );
					session.persist( entry );
				}
			} );

			scope.inTransaction( session -> {
				assertThat( session.createQuery(
						"from DaylightSavingTimeEntry order by id", TimeEntry.class
				).getResultList() ).extracting( entry -> entry.localTime ).containsExactlyElementsOf( expected );

				final List<LocalDateTime> stored = session.doReturningWork( connection -> {
					final List<LocalDateTime> values = new ArrayList<>();
					try ( var statement = connection.prepareStatement(
							"select local_time from hhh16722_time order by id"
					); var rows = statement.executeQuery() ) {
						while ( rows.next() ) {
							values.add( rows.getObject( 1, LocalDateTime.class ) );
						}
					}
					return values;
				} );
				assertThat( stored ).containsExactlyElementsOf( expected );
			} );
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	public static class DirectLocalDateTimeSupport implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.getDirectJavaTimeJdbcSupport().supports( LocalDateTime.class );
		}
	}

	@Entity(name = "DaylightSavingTimeEntry")
	@Table(name = "hhh16722_time")
	public static class TimeEntry {
		@Id
		private Integer id;

		@Column(name = "local_time")
		private LocalDateTime localTime;
	}
}
