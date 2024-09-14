/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.spi;

/**
 * BatchLoader specialization for {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entity} fetching
 *
 * @author Steve Ebersole
 */
public interface EntityBatchLoader<T> extends BatchLoader, SingleIdEntityLoader<T> {
}
