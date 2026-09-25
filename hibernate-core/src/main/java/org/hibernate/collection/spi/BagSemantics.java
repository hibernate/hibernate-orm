package org.hibernate.collection.spi;

import java.util.Collection;

/**
 * @author Andrea Boriero
 */
public interface BagSemantics<BE extends Collection<E>, E> extends CollectionSemantics<BE,E> {
}
