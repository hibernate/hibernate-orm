/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import org.hibernate.FetchMode;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.PropertyMapping;

/**
 * A collection role that may be queried or loaded by outer join.
 * @author Gavin King
 */
public interface QueryableCollection extends PropertyMapping, Joinable, CollectionPersister {
	/**
	 * Generate a list of collection index and element columns
	 */
	String selectFragment(String alias, String columnSuffix);
	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation)
	 */
	String[] getIndexColumnNames();
	/**
	 * Get the index formulas if this is an indexed collection 
	 * (optional operation)
	 */
	String[] getIndexFormulas();
	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation),
	 * aliased by the given table alias
	 */
	String[] getIndexColumnNames(String alias);
	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association),
	 * aliased by the given table alias
	 */
	String[] getElementColumnNames(String alias);
	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association)
	 */
	String[] getElementColumnNames();
	/**
	 * Get the order by SQL
	 */
	String getSQLOrderByString(String alias);

	/**
	 * Get the order-by to be applied at the target table of a many to many
	 *
	 * @param alias The alias for the many-to-many target table
	 * @return appropriate order-by fragment or empty string.
	 */
	String getManyToManyOrderByString(String alias);

	/**
	 * Does this collection role have a where clause filter?
	 */
	boolean hasWhere();
	/**
	 * Get the persister of the element class, if this is a
	 * collection of entities (optional operation).  Note that
	 * for a one-to-many association, the returned persister
	 * must be {@code OuterJoinLoadable}.
	 */
	EntityPersister getElementPersister();
	/**
	 * Should we load this collection role by outerjoining?
	 */
	FetchMode getFetchMode();

}
