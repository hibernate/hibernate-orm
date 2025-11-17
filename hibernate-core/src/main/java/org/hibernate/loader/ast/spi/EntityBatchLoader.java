/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * BatchLoader specialization for {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entity} fetching
 *
 * @author Steve Ebersole
 */
public interface EntityBatchLoader<T> extends BatchLoader, SingleIdEntityLoader<T> {
}
