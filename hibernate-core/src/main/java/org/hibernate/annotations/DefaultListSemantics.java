package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies the default semantics of persistent [java.util.List] attributes
/// without explicit collection classification or ordering metadata.
///
/// The annotation on the package declaring an attribute takes precedence over
/// the annotation on its module. Package defaults do not apply to subpackages.
/// Inherited attributes and attributes of embeddables use their declaring
/// type's package and module, not those of the containing entity.
///
/// Explicit mapping metadata takes precedence over this default. In particular,
/// in the absence of explicit list-index metadata, lists with
/// [jakarta.persistence.OrderBy] or [SQLOrder], and unowned to-many associations,
/// retain bag semantics.
///
/// In the absence of this annotation, the configured implicit list semantics
/// apply, defaulting to [Classification#BAG].
///
/// @since 8.0
/// @author Steve Ebersole
@Target({PACKAGE, MODULE})
@Retention(RUNTIME)
@Discoverable
@Incubating(since = "8.0")
public @interface DefaultListSemantics {
	/// The semantics to use for eligible list attributes.
	Classification value();

	/// The supported defaults for list attributes.
	enum Classification {
		/// Element positions are not persistent.
		BAG,
		/// Element positions are persistent, using an implicit order column.
		LIST
	}
}
