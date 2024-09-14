/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.spi;

/**
 * Commonality for multi-loading an {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entity}
 *
 * @param <T> The loaded model part
 */
public interface EntityMultiLoader<T> extends EntityLoader, MultiKeyLoader {
}
