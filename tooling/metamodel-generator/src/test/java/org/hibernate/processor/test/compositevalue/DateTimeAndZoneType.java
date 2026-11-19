package org.hibernate.processor.test.compositevalue;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class DateTimeAndZoneType implements CompositeUserType<ZonedDateTime> {
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
