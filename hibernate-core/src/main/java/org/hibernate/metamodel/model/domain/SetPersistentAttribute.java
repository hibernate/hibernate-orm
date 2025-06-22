/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Set;
import jakarta.persistence.metamodel.SetAttribute;

/**
 * Hibernate extension to the JPA {@link SetAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface SetPersistentAttribute<D,E> extends SetAttribute<D,E>, PluralPersistentAttribute<D,Set<E>,E> {
}
