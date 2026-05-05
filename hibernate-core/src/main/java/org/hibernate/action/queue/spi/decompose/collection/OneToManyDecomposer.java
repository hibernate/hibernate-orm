/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.collection;

import org.hibernate.Incubating;

/// Marker interface for one-to-many collection decomposers
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface OneToManyDecomposer extends CollectionDecomposer {
}
