/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * A loader (initialization) for collections
 *
 * @author Steve Ebersole
 */
public interface CollectionLoader extends Loader {
	@Nonnull
	@Override
	PluralAttributeMapping getLoadable();

	/**
	 * Load a collection by its key (not necessarily the same as its owner's PK).
	 */
	@Nonnull
	PersistentCollection<?> load(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session);

	//TODO support 'readOnly' collection loading

}
