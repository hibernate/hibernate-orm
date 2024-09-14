/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
