/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;

/// A settled table dependency, distinguishing named tables from inline views.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public sealed interface TableNamingInput permits NamedTableNamingInput, InlineViewNamingInput {
	LogicalName logicalName();
}
