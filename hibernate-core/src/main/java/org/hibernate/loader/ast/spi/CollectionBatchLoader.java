/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * BatchLoader specialization for {@linkplain org.hibernate.metamodel.mapping.PluralAttributeMapping collection} fetching
 *
 * @author Steve Ebersole
 */
public interface CollectionBatchLoader extends BatchLoader, CollectionLoader {
}
