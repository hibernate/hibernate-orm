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
	public abstract String selectFragment(String alias, String columnSuffix);
	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation)
	 */
	public abstract String[] getIndexColumnNames();
	/**
	 * Get the index formulas if this is an indexed collection 
	 * (optional operation)
	 */
	public abstract String[] getIndexFormulas();
	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation),
	 * aliased by the given table alias
	 */
	public abstract String[] getIndexColumnNames(String alias);
	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association),
	 * aliased by the given table alias
	 */
	public abstract String[] getElementColumnNames(String alias);
	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association)
	 */
	public abstract String[] getElementColumnNames();
	/**
	 * Get the order by SQL
	 */
	public abstract String getSQLOrderByString(String alias);

	/**
	 * Get the order-by to be applied at the target table of a many to many
	 *
	 * @param alias The alias for the many-to-many target table
	 * @return appropriate order-by fragment or empty string.
	 */
	public abstract String getManyToManyOrderByString(String alias);

	/**
	 * Does this collection role have a where clause filter?
	 */
	public abstract boolean hasWhere();
	/**
	 * Get the persister of the element class, if this is a
	 * collection of entities (optional operation).  Note that
	 * for a one-to-many association, the returned persister
	 * must be <tt>OuterJoinLoadable</tt>.
	 */
	public abstract EntityPersister getElementPersister();
	/**
	 * Should we load this collection role by outerjoining?
	 */
	public abstract FetchMode getFetchMode();

}
