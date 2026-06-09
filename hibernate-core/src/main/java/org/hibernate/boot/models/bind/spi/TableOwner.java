/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

/// Marker for categorized or binding objects that own a table reference.
///
/// Binding state uses table owners to associate a produced {@link TableReference}
/// with the model object whose mapping declared or implied that table.
///
/// @since 9.0
/// @author Steve Ebersole
public interface TableOwner {
}
