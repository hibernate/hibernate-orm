package org.hibernate.orm.test.mapping.basic;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-21189")
@DomainModel(annotatedClasses = TimeZoneStoragePropertyAccessTest.PropertyAccess.class)
public class TimeZoneStoragePropertyAccessTest {
	private static final OffsetDateTime OFFSET = OffsetDateTime.parse( "2026-01-15T12:30:00+05:45" );
	private static final OffsetTime TIME = OFFSET.toOffsetTime();
	private static final ZonedDateTime ZONED = OFFSET.toZonedDateTime();

	@Test
	void columns(DomainModelScope scope) {
		scope.withHierarchy( PropertyAccess.class, entity -> {
			assertThat( entity.getTable().getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "id", "zoned", "zoned_tz", "offsetDateTime", "offsetDateTime_tz", "offsetTime", "offsetTime_tz" );
		} );
	}

	@Test
	@SessionFactory
	void roundTrip(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PropertyAccess entity = new PropertyAccess();
			entity.setId( 1 );
			entity.setOffsetDateTime( OFFSET );
			entity.setOffsetTime( TIME );
			entity.setZoned( ZONED );
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			final PropertyAccess entity = session.find( PropertyAccess.class, 1 );
			assertThat( entity.getOffsetDateTime() ).isEqualTo( OFFSET );
			assertThat( entity.getOffsetTime() ).isEqualTo( TIME );
			assertThat( entity.getZoned() ).isEqualTo( ZONED );
			session.remove( entity );
		} );
	}

	@Test
	@SessionFactory
	void nullValues(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PropertyAccess entity = new PropertyAccess();
			entity.setId( 3 );
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			final PropertyAccess entity = session.find( PropertyAccess.class, 3 );
			assertThat( entity.getOffsetDateTime() ).isNull();
			assertThat( entity.getOffsetTime() ).isNull();
			assertThat( entity.getZoned() ).isNull();
			session.remove( entity );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = { PropertyAccess.class, FieldAccess.class })
	@SessionFactory
	void mixedAccess(SessionFactoryScope scope) {
		roundTrip( scope );
		scope.inTransaction( session -> {
			final FieldAccess entity = new FieldAccess();
			entity.id = 2;
			entity.offsetDateTime = OFFSET;
			entity.offsetTime = TIME;
			entity.zoned = ZONED;
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			final FieldAccess entity = session.find( FieldAccess.class, 2 );
			assertThat( entity.offsetDateTime ).isEqualTo( OFFSET );
			assertThat( entity.offsetTime ).isEqualTo( TIME );
			assertThat( entity.zoned ).isEqualTo( ZONED );
			session.remove( entity );
		} );
	}

	@Entity(name = "PropertyAccess")
	public static class PropertyAccess {
		private Integer id;
		private ZonedDateTime zoned;
		private OffsetDateTime offsetDateTime;
		private OffsetTime offsetTime;

		@Id
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		public ZonedDateTime getZoned() {
			return zoned;
		}
		public void setZoned(ZonedDateTime zoned) {
			this.zoned = zoned;
		}

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		public OffsetDateTime getOffsetDateTime() {
			return offsetDateTime;
		}
		public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
			this.offsetDateTime = offsetDateTime;
		}

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		public OffsetTime getOffsetTime() {
			return offsetTime;
		}
		public void setOffsetTime(OffsetTime offsetTime) {
			this.offsetTime = offsetTime;
		}
	}

	@Entity(name = "FieldAccess")
	public static class FieldAccess {
		@Id
		private Integer id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		private ZonedDateTime zoned;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		private OffsetDateTime offsetDateTime;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		private OffsetTime offsetTime;
	}
}
