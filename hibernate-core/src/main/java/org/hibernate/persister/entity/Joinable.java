/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.metamodel.mapping.Restrictable;

/**
 * Anything that can be loaded by outer join - namely
 * persisters for classes or collections.
 *
 * @author Gavin King
 */
public interface Joinable extends Restrictable {
	//should this interface extend PropertyMapping?

	/**
	 * An identifying name; a class name or collection role name.
	 */
	String getName();
	/**
	 * The table to join to.
	 */
	String getTableName();

	/**
	 * The columns to join on
	 */
	String[] getKeyColumnNames();

	/**
	 * Is this instance actually a CollectionPersister?
	 */
	boolean isCollection();

	/**
	 * Very, very, very ugly...
	 *
	 * @return Does this persister "consume" entity column aliases in the result
	 * set?
	 */
	boolean consumesEntityAlias();

	/**
	 * Very, very, very ugly...
	 *
	 * @return Does this persister "consume" collection column aliases in the result
	 * set?
	 */
	boolean consumesCollectionAlias();
}
