/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;

/// EntityMultiLoader implementation based on [identifier][org.hibernate.KeyType#IDENTIFIER].
///
/// @see org.hibernate.Session#findMultiple
///
/// @author Steve Ebersole
public interface MultiIdEntityLoader<T> extends EntityMultiLoader<T> {
	/**
	 * Load multiple entities by id.  The exact result depends on the passed options.
	 */
	@Nonnull
	<K> List<T> load(@Nonnull K[] ids, @Nonnull MultiIdLoadOptions options, @Nonnull SharedSessionContractImplementor session);
}
