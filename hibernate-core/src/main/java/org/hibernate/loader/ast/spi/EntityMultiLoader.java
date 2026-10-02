package org.hibernate.loader.ast.spi;

/**
 * Commonality for multi-loading an {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entity}
 *
 * @param <T> The loaded model part
 */
public interface EntityMultiLoader<T> extends EntityLoader, MultiKeyLoader {
}
