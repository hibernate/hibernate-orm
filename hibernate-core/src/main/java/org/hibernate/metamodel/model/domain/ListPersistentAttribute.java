/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;
import jakarta.persistence.metamodel.ListAttribute;

import org.hibernate.query.sqm.SqmPathSource;

/**
 * Hibernate extension to the JPA {@link ListAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface ListPersistentAttribute<D,E> extends ListAttribute<D,E>, PluralPersistentAttribute<D,List<E>,E> {
	SqmPathSource<Integer> getIndexPathSource();
}
