package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.models.spi.ModelsContext;

/// Mutable model representation of [TimeZoneAdjustment].
///
/// @author Steve Ebersole
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class TimeZoneAdjustmentAnnotation implements TimeZoneAdjustment {
	private String value;

	public TimeZoneAdjustmentAnnotation(ModelsContext context) {
	}

	public TimeZoneAdjustmentAnnotation(TimeZoneAdjustment annotation, ModelsContext context) {
		value = annotation.value();
	}

	public TimeZoneAdjustmentAnnotation(Map<String, Object> values, ModelsContext context) {
		value = (String) values.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TimeZoneAdjustment.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}
}
