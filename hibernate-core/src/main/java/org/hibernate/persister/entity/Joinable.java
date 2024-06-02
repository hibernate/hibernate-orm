/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

/**
 * Anything that can be loaded by outer join - namely
 * persisters for classes or collections.
 *
 * @author Gavin King
 *
 * @deprecated Use {@link org.hibernate.metamodel.mapping.ModelPartContainer},
 * {@link org.hibernate.sql.ast.tree.from.TableGroupProducer} and/or
 * {@link org.hibernate.sql.ast.tree.from.TableGroupJoinProducer} instead
 * depending on need
 */
@Deprecated(since = "6", forRemoval = true)
public interface Joinable {
}
