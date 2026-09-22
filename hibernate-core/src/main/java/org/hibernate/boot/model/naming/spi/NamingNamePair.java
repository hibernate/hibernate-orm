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
/// All reference components are non-null.
///
/// @param logicalName The non-null logical name selected by the mapping reference or declaration, retaining
/// its explicit/implicit provenance and requested quoting
/// @param physicalName The non-null finalized physical name of the same object, retaining physical quoting;
/// it is not a second candidate logical name
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record NamingNamePair(LogicalName logicalName, PhysicalName physicalName) {
	public NamingNamePair {
		requireNonNull( logicalName, "logicalName" );
		requireNonNull( physicalName, "physicalName" );
	}
}
