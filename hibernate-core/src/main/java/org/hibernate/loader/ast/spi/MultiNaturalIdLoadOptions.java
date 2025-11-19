/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/// Encapsulation of the options for loading multiple entities (of a type)
/// by [natural-id][org.hibernate.KeyType#NATURAL].
///
/// @see MultiNaturalIdLoader
///
/// @author Steve Ebersole
public interface MultiNaturalIdLoadOptions extends MultiLoadOptions {
}
