/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.Remove;

/**
 * Contract for all things that know how to map a property to the needed bits of SQL.
 * <p>
 * The column/formula fragments that represent a property in the table defining the property be obtained by
 * calling {@link #toColumns(String)}.
 *
 * <p>
 * Note, the methods here are generally ascribed to accept "property paths".  That is a historical necessity because
 * of how Hibernate originally understood composites (embeddables) internally.  That is in the process of changing
 * as Hibernate has added {@link org.hibernate.persister.collection.CompositeElementPropertyMapping}
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated This interface is obsolete
 */
@Deprecated(since = "6", forRemoval = true)
@Remove
public interface PropertyMapping {
}
