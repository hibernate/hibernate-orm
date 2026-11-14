package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies mapping-specific timezone adjustment on both write and read.
///
/// Declarations use the mapped member, its declaring class and superclass chain,
/// exact declaring package, then declaring module, in that order.
///
/// @implSpec Only supported with [org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION] enabled.
///
/// @author Steve Ebersole
/// @since 8.1
@Incubating(since = "8.1")
@Retention(RUNTIME)
@Target({ FIELD, METHOD, TYPE, PACKAGE, MODULE })
public @interface TimeZoneAdjustment {
	/// Explicitly disables a broader adjustment declaration.
	String NONE = "<none>";

	/// A [java.time.ZoneId] identifier, fixed offset, or [#NONE].
	String value();
}
