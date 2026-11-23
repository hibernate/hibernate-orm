package org.hibernate.orm.test.jpa.metamodel;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.TemporalAttribute;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.jpa.metamodel.TimeZoneColumnTemporalAttributeTest_.TzEvent_;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A temporal attribute is a {@link TemporalAttribute}, and its static
 * metamodel field is populated, even when its offset or zone is stored
 * in a separate column.
 */
@JiraKey("HHH-21120")
@DomainModel(annotatedClasses = TimeZoneColumnTemporalAttributeTest.TzEvent.class)
@SessionFactory
public class TimeZoneColumnTemporalAttributeTest {

	// 08:00Z
	private static final OffsetDateTime EARLY = OffsetDateTime.of( 2020, 1, 1, 10, 0, 0, 0, ZoneOffset.ofHours( 2 ) );
	// 09:00Z
	private static final OffsetDateTime LATE = OffsetDateTime.of( 2020, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC );
	private static final OffsetDateTime BETWEEN = OffsetDateTime.of( 2020, 1, 1, 8, 30, 0, 0, ZoneOffset.UTC );

	@BeforeAll
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TzEvent( 1L, EARLY ) );
			session.persist( new TzEvent( 2L, LATE ) );
		} );
	}

	@Test
	public void attributesAreTemporal(SessionFactoryScope scope) {
		final var event = scope.getSessionFactory().getMetamodel().entity( TzEvent.class );
		for ( String name : List.of( "offsetDateTime", "zonedDateTime", "offsetTime", "auto" ) ) {
			assertInstanceOf( TemporalAttribute.class, event.getAttribute( name ), name );
		}
		assertSame( event.getAttribute( "offsetDateTime" ), TzEvent_.offsetDateTime );
		assertSame( event.getAttribute( "zonedDateTime" ), TzEvent_.zonedDateTime );
		assertSame( event.getAttribute( "offsetTime" ), TzEvent_.offsetTime );
		assertSame( event.getAttribute( "auto" ), TzEvent_.auto );
	}

	@Test
	public void compareViaTemporalAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();

			final var after = builder.createQuery( Long.class );
			final var afterRoot = after.from( TzEvent.class );
			after.select( afterRoot.get( TzEvent_.id ) )
					.where( afterRoot.get( TzEvent_.offsetDateTime ).greaterThan( BETWEEN ) );
			assertEquals( List.of( 2L ), session.createQuery( after ).getResultList() );

			final var before = builder.createQuery( Long.class );
			final var beforeRoot = before.from( TzEvent.class );
			before.select( beforeRoot.get( TzEvent_.id ) )
					.where( beforeRoot.get( TzEvent_.zonedDateTime ).lessThan( BETWEEN.toZonedDateTime() ) );
			assertEquals( List.of( 1L ), session.createQuery( before ).getResultList() );

			final var auto = builder.createQuery( Long.class );
			final var autoRoot = auto.from( TzEvent.class );
			auto.select( autoRoot.get( TzEvent_.id ) )
					.where( autoRoot.get( TzEvent_.auto ).greaterThan( BETWEEN ) );
			assertEquals( List.of( 2L ), session.createQuery( auto ).getResultList() );
		} );
	}

	@Test
	public void orderViaTemporalAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( TzEvent.class );
			query.select( root.get( TzEvent_.id ) )
					.orderBy( root.get( TzEvent_.offsetDateTime ).desc() );
			assertEquals( List.of( 2L, 1L ), session.createQuery( query ).getResultList() );
		} );
	}

	@Entity(name = "TzEvent")
	public static class TzEvent {
		@Id
		Long id;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		OffsetDateTime offsetDateTime;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		ZonedDateTime zonedDateTime;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		OffsetTime offsetTime;

		// a composite only on databases with no native time zone support
		@TimeZoneStorage(TimeZoneStorageType.AUTO)
		OffsetDateTime auto;

		TzEvent() {
		}

		TzEvent(Long id, OffsetDateTime dateTime) {
			this.id = id;
			this.offsetDateTime = dateTime;
			this.zonedDateTime = dateTime.atZoneSameInstant( ZoneId.of( "Europe/Madrid" ) );
			this.offsetTime = dateTime.toOffsetTime();
			this.auto = dateTime;
		}
	}
}
