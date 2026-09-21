package org.hibernate.boot.model.relational;

import org.hibernate.relational.naming.spi.PhysicalName;

/// Finalized physical catalog/schema qualifiers, each of which may be null.
///
/// @author Steve Ebersole
public record PhysicalNamespaceName(PhysicalName catalog, PhysicalName schema) {
}
