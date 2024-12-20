/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.collection.spi.CollectionSemantics;

/**
 * MappingType descriptor for the collection Java type (List, Set, etc)
 *
 * @author Steve Ebersole
 */
public interface CollectionMappingType<C> extends MappingType {
	CollectionSemantics<C,?> getCollectionSemantics();
}
