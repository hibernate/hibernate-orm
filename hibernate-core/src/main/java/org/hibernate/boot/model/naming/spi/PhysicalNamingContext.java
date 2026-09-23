/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Construction capabilities supplied to a physical naming strategy.
/// Mapping quoting is present on the logical input. Hibernate applies global and
/// automatic quoting to the returned physical name without removing input quoting.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public interface PhysicalNamingContext {
	PhysicalName.Factory getPhysicalNameFactory();
}
