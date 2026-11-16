package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TemporalPreservationPolicies;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

/// Mutable model representation of [TemporalPreservationPolicies].
///
/// @author Steve Ebersole
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class TemporalPreservationPoliciesAnnotation implements TemporalPreservationPolicies, RepeatableContainer<TemporalPreservationPolicy> {
	private TemporalPreservationPolicy[] value;

	public TemporalPreservationPoliciesAnnotation(ModelsContext context) {
	}

	public TemporalPreservationPoliciesAnnotation(TemporalPreservationPolicies annotation, ModelsContext context) {
		value = annotation.value();
	}

	public TemporalPreservationPoliciesAnnotation(Map<String, Object> values, ModelsContext context) {
		value = (TemporalPreservationPolicy[]) values.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TemporalPreservationPolicies.class;
	}

	@Override
	public TemporalPreservationPolicy[] value() {
		return value;
	}

	public void value(TemporalPreservationPolicy[] value) {
		this.value = value;
	}
}
