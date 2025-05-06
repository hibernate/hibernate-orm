/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;
import jakarta.persistence.metamodel.ListAttribute;

/**
 * Hibernate extension to the JPA {@link ListAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface ListPersistentAttribute<D,E> extends ListAttribute<D,E>, PluralPersistentAttribute<D,List<E>,E> {
	PathSource<Integer> getIndexPathSource();
}
