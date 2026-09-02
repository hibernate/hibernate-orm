/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.SPI;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Contributes custom type contracts and descriptors to a
/// [org.hibernate.type.spi.TypeConfiguration] through [TypeContributions].
///
/// Providers normally expose an implementation as a Java
/// [java.util.ServiceLoader] service. An application may instead supply one
/// programmatically through
/// [org.hibernate.cfg.Configuration#registerTypeContributor(TypeContributor)]
/// or [org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)]. JPA
/// bootstrap may list contributors using
/// [org.hibernate.jpa.boot.spi.JpaSettings#TYPE_CONTRIBUTORS].
///
/// A contributor must complete registration during [#contribute] and must not
/// retain the supplied contributions or service registry.
///
/// @see org.hibernate.type.spi.TypeConfiguration
/// @see org.hibernate.cfg.Configuration#registerTypeContributor(TypeContributor)
/// @see org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)
///
/// @author Steve Ebersole
@JavaServiceLoadable
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TypeContributor {
	/// Contribute types during metadata bootstrap.
	///
	/// @param typeContributions the callback for adding contributed types
	/// @param serviceRegistry the service registry
	void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry);

	/**
	 * Determines order in which the contributions will be applied
	 * (lowest ordinal first).
	 * <p>
	 * The range 0-500 is reserved for Hibernate, range 500-1000 for libraries and
	 * 1000-Integer.MAX_VALUE for user-defined TypeContributors.
	 * <p>
	 * Contributions from higher precedence contributors (higher numbers) effectively override
	 * contributions from lower precedence.  E.g. if a contributor with precedence 2000 contributes
	 * some type, that will override Hibernate's standard type of that name.
	 *
	 * @return the ordinal for this TypeContributor
	 */
	default int ordinal(){
		return 1000;
	}
}
