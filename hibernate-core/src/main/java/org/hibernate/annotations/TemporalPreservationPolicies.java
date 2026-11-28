package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Repeatable container for [TemporalPreservationPolicy].
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@Retention(RUNTIME)
@Target({ FIELD, METHOD, TYPE, PACKAGE, MODULE })
public @interface TemporalPreservationPolicies {
	TemporalPreservationPolicy[] value();
}
