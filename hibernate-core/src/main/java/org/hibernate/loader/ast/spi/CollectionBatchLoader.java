package org.hibernate.loader.ast.spi;

/**
 * BatchLoader specialization for {@linkplain org.hibernate.metamodel.mapping.PluralAttributeMapping collection} fetching
 *
 * @author Steve Ebersole
 */
public interface CollectionBatchLoader extends BatchLoader, CollectionLoader {
}
