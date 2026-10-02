package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.metamodel.model.domain.ListPersistentAttribute;

import java.util.List;

public interface SqmListPersistentAttribute<D, E>
		extends ListPersistentAttribute<D, E>, SqmPluralPersistentAttribute<D, List<E>, E> {
}
