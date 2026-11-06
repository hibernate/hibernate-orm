package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.models.spi.ModelsContext;

/// Mutable model representation of [TimeZoneRepresentation].
///
/// @author Steve Ebersole
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class TimeZoneRepresentationAnnotation implements TimeZoneRepresentation {
	private TimeZoneRepresentation.Type value;

	public TimeZoneRepresentationAnnotation(ModelsContext context) {
	}

	public TimeZoneRepresentationAnnotation(TimeZoneRepresentation annotation, ModelsContext context) {
		value = annotation.value();
	}

	public TimeZoneRepresentationAnnotation(Map<String, Object> values, ModelsContext context) {
		value = (TimeZoneRepresentation.Type) values.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TimeZoneRepresentation.class;
	}

	@Override
	public TimeZoneRepresentation.Type value() {
		return value;
	}

	public void value(TimeZoneRepresentation.Type value) {
		this.value = value;
	}
}
