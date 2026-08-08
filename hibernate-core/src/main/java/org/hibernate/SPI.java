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
/// SPI classification is distinct from application API classification. Public
/// visibility of an SPI element does not, by itself, make that element supported
/// for direct use by provider code.
///
/// The roles [USE][Role#USE], [IMPLEMENT][Role#IMPLEMENT], and
/// [SUPPLY][Role#SUPPLY] are independent. No role implies another role.
///
/// @implNote Runtime retention allows build tooling to report and validate SPI
/// classifications from compiled metadata.
///
/// @since 8.1
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
	enum Role {
		/// Provider code may reference, invoke, or instantiate the contract while
		/// implementing an extension.
		USE,

		/// Provider code may implement an interface, subclass a class, or override
		/// a method represented by the contract.
		IMPLEMENT,

		/// Provider code may make an implementation, instance, or value represented
		/// by the contract available to Hibernate.
		SUPPLY
	}
}
