/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
