/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * Commonality for multi-loading an {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entity}
 *
 * @param <T> The loaded model part
 */
public interface EntityMultiLoader<T> extends EntityLoader, MultiKeyLoader {
}
