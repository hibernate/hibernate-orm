package org.hibernate.orm.test.jpa.metamodel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Attributes whose Java type is basic-like (temporal, comparable) but which
 * are mapped as composite values must have a static metamodel field type
 * which the runtime attribute can be assigned to.
 */
@JiraKey("HHH-21120")
@DomainModel(annotatedClasses = {
		CompositeValuedStaticMetamodelTest.Event.class,
		CompositeValuedStaticMetamodelTest.Shipment.class
})
@SessionFactory
public class CompositeValuedStaticMetamodelTest {

	@Test
	public void staticMetamodelIsPopulated(SessionFactoryScope scope) {
		final var metamodel = scope.getSessionFactory().getMetamodel();
		final var event = metamodel.entity( Event.class );
		final var details = metamodel.embeddable( Details.class );

		assertNotNull( CompositeValuedStaticMetamodelTest_.Event_.startsAt );
		assertNotNull( CompositeValuedStaticMetamodelTest_.Event_.endsAt );
		assertNotNull( CompositeValuedStaticMetamodelTest_.Event_.price );
		assertNotNull( CompositeValuedStaticMetamodelTest_.Details_.shippedAt );

		assertSame( event.getAttribute( "startsAt" ), CompositeValuedStaticMetamodelTest_.Event_.startsAt );
		assertSame( event.getAttribute( "endsAt" ), CompositeValuedStaticMetamodelTest_.Event_.endsAt );
		assertSame( event.getAttribute( "price" ), CompositeValuedStaticMetamodelTest_.Event_.price );
		assertSame( details.getAttribute( "shippedAt" ), CompositeValuedStaticMetamodelTest_.Details_.shippedAt );
	}

	@Entity(name = "Event")
	@Table(name = "event_m")
	public static class Event {
		@Id
		Long id;

		@CompositeType(DateTimeAndZoneType.class)
		ZonedDateTime startsAt;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		OffsetDateTime endsAt;

		@Embedded
		Money price;
	}

	@Entity(name = "Shipment")
	@Table(name = "shipment_m")
	public static class Shipment {
		@Id
		Long id;

		@Embedded
		Details details;
	}

	@Embeddable
	public static class Details {
		String note;

		@CompositeType(DateTimeAndZoneType.class)
		ZonedDateTime shippedAt;
	}

	// deliberately not annotated @Embeddable
	public static class Money implements Comparable<Money> {
		BigDecimal amount;
		String currency;

		@Override
		public int compareTo(Money that) {
			return amount.compareTo( that.amount );
		}
	}

	public static class DateTimeAndZoneType implements CompositeUserType<ZonedDateTime> {
		public static class Mapper {
			LocalDateTime dateTime;
			String zone;
		}

		@Override
		public Object getPropertyValue(ZonedDateTime component, int property) {
			return property == 0 ? component.toLocalDateTime() : component.getZone().getId();
		}

		@Override
		public ZonedDateTime instantiate(ValueAccess values) {
			final LocalDateTime dateTime = values.getValue( 0, LocalDateTime.class );
			final String zone = values.getValue( 1, String.class );
			return dateTime == null ? null : ZonedDateTime.of( dateTime, ZoneId.of( zone ) );
		}

		@Override
		public Class<?> embeddable() {
			return Mapper.class;
		}

		@Override
		public Class<ZonedDateTime> returnedClass() {
			return ZonedDateTime.class;
		}

		@Override
		public boolean equals(ZonedDateTime x, ZonedDateTime y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(ZonedDateTime x) {
			return Objects.hashCode( x );
		}

		@Override
		public ZonedDateTime deepCopy(ZonedDateTime value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(ZonedDateTime value) {
			return value;
		}

		@Override
		public ZonedDateTime assemble(Serializable cached, Object owner) {
			return (ZonedDateTime) cached;
		}

		@Override
		public ZonedDateTime replace(ZonedDateTime detached, ZonedDateTime managed, Object owner) {
			return detached;
		}
	}
}
