package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.models.spi.ModelsContext;

/// Mutable model representation of [TemporalPreservationPolicy].
///
/// @author Steve Ebersole
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class TemporalPreservationPolicyAnnotation implements TemporalPreservationPolicy {
	private Type value;
	private Class<?>[] types = new Class<?>[0];

	public TemporalPreservationPolicyAnnotation(ModelsContext context) {
	}

	public TemporalPreservationPolicyAnnotation(TemporalPreservationPolicy annotation, ModelsContext context) {
		value = annotation.value();
		types = annotation.types();
	}

	public TemporalPreservationPolicyAnnotation(Map<String, Object> values, ModelsContext context) {
		value = (Type) values.get( "value" );
		types = (Class<?>[]) values.getOrDefault( "types", new Class<?>[0] );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TemporalPreservationPolicy.class;
	}

	@Override
	public Type value() {
		return value;
	}

	public void value(Type value) {
		this.value = value;
	}

	@Override
	public Class<?>[] types() {
		return types;
	}

	public void types(Class<?>[] types) {
		this.types = types;
	}
}
