/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import static java.util.Objects.requireNonNull;

/// The selected logical name and finalized physical name of one dependency.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record NamingNamePair(LogicalName logicalName, PhysicalName physicalName) {
	public NamingNamePair {
		requireNonNull( logicalName, "logicalName" );
		requireNonNull( physicalName, "physicalName" );
	}
}
