/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Selects a provider-owned Dialect class for an explicit short name.
///
/// Expose an implementation through the Java [java.util.ServiceLoader]
/// facility. Return `null` for an unrecognized non-null name so another
/// selector, followed by Hibernate's standard selector, may try the same text.
/// Provider names should be unique; do not rely on service-provider ordering
/// to resolve a collision.
///
/// @author Christian Beikov
/// @see #resolve(String)
@JavaServiceLoadable
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface DialectSelector extends Service {
	/// Supply the Dialect implementation class selected by the exact configured
	/// name.
	///
	/// Do not construct a Dialect merely to answer this lookup. Return `null`
	/// when the non-null name is not recognized.
	///
	/// @param name the non-null explicit Dialect name
	/// @return the selected Dialect class, or `null` when this selector declines
	/// the name
	/// @see Dialect
	@SPI({ USE, IMPLEMENT, SUPPLY })
	Class<? extends Dialect> resolve(String name);
}
