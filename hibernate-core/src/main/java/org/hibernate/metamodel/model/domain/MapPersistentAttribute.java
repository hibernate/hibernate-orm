/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Map;
import jakarta.persistence.metamodel.MapAttribute;

import org.hibernate.query.sqm.SqmPathSource;

/**
 * Hibernate extension to the JPA {@link MapAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface MapPersistentAttribute<D,K,V> extends MapAttribute<D, K, V>, PluralPersistentAttribute<D,Map<K,V>,V> {
	SqmPathSource<K> getKeyPathSource();

	@Override
	SimpleDomainType<K> getKeyType();
}
