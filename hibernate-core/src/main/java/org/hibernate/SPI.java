/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Identifies a contract intended for participation by an external service
/// provider and specifies how provider code is expected to participate.
///
/// SPI classification may also be implied by placement in an `.spi` package.
/// This annotation makes the supported provider roles explicit for a package,
/// type, or member, including exceptions to package-derived classification.
///
/// SPI classification is distinct from application API classification. Public
/// visibility of an SPI element does not, by itself, make that element supported
/// for direct use by provider code.
///
/// The roles [USE][Role#USE], [IMPLEMENT][Role#IMPLEMENT], and
/// [SUPPLY][Role#SUPPLY] are independent. No role implies another role.
/// Compatibility must be evaluated against every applicable role.
///
/// Unless the contract is [incubating][Incubating], SPI support applies across
/// the maintenance releases of one `X.Y` release family. Within that family,
/// Hibernate aims to preserve both linkage of already-compiled providers and
/// recompilation of their source. Compatibility across different `X.Y`
/// families is not guaranteed. See the
/// [Hibernate compatibility policy](https://hibernate.org/support/compatibility-policy/#compatibility-api-spi).
///
/// @implNote Runtime retention allows build tooling to report and validate SPI
/// classifications from compiled metadata.
///
/// @since 8.0
/// @author Steve Ebersole
@Target({ PACKAGE, TYPE, METHOD, FIELD, CONSTRUCTOR, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Documented
public @interface SPI {
	/// The ways external provider code is intended to participate in the
	/// annotated contract.
	///
	/// @return the independent provider roles; defaults to [Role#USE]
	Role[] value() default Role.USE;

	/// Describes how external provider code participates in an SPI contract.
	///
	/// Compatibility is determined by the complete role set. For example, adding
	/// a method might be additive for code which only [uses][#USE] a type, while
	/// breaking code which [implements][#IMPLEMENT] that same type.
	enum Role {
		/// Allows external provider code to consume the contract while implementing
		/// an integration. Provider code may name the contract in source and binary
		/// signatures and, where the corresponding member is exposed, reference,
		/// invoke, or instantiate it.
		///
		/// `USE` does not authorize provider code to implement an interface, subclass
		/// a class, override a method, or supply a value to Hibernate. Those
		/// directions require [IMPLEMENT][#IMPLEMENT] or [SUPPLY][#SUPPLY].
		///
		/// Within one supported `X.Y` release family, existing usable types and
		/// members are expected to retain source- and binary-compatible signatures.
		/// New members may be added when they remain compatible for every applicable
		/// role. Removal, renaming, incompatible signature change, or reduced
		/// accessibility of an existing usable member is incompatible.
		USE,

		/// Allows external provider code to implement an interface, subclass a
		/// class, or override a method represented by the contract.
		///
		/// For an implementable class, the supported subclass surface includes its
		/// exposed public and protected overridable instance methods. Supported
		/// constructors are classified independently; `IMPLEMENT` on a class does
		/// not make every constructor a provider contract.
		///
		/// Within one supported `X.Y` release family, existing provider
		/// implementations and subclasses are expected to remain source and binary
		/// compatible. Adding an abstract method or otherwise imposing a new
		/// implementation obligation is incompatible. Adding a default or concrete
		/// method requires source-, binary-, and method-collision analysis. Removing
		/// or finalizing an overridable member, reducing its accessibility, or
		/// changing its signature incompatibly is also incompatible.
		IMPLEMENT,

		/// Allows external provider code to make an implementation, instance, or
		/// value represented by the contract available to Hibernate through a
		/// documented supply point.
		///
		/// `SUPPLY` describes direction, not discovery, construction, or
		/// implementation permission. A provider needs [IMPLEMENT][#IMPLEMENT] to
		/// implement or subclass a supplied contract and [USE][#USE] to consume APIs
		/// involved in producing the supplied value.
		///
		/// Within one supported `X.Y` release family, an existing supply point and
		/// the accepted value contract are expected to remain source and binary
		/// compatible. New optional supply points may be added. Requiring an existing
		/// provider to supply a new value, removing a supply point, or changing its
		/// accepted contract incompatibly is not compatible.
		SUPPLY
	}
}
