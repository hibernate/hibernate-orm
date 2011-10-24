/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.persister.entity;
import java.util.Map;

import org.hibernate.MappingException;

/**
 * Anything that can be loaded by outer join - namely
 * persisters for classes or collections.
 *
 * @author Gavin King
 */
public interface Joinable {
	//should this interface extend PropertyMapping?

	/**
	 * An identifying name; a class name or collection role name.
	 */
	public String getName();
	/**
	 * The table to join to.
	 */
	public String getTableName();

	/**
	 * All columns to select, when loading.
	 */
	public String selectFragment(Joinable rhs, String rhsAlias, String lhsAlias, String currentEntitySuffix, String currentCollectionSuffix, boolean includeCollectionColumns);

	/**
	 * Get the where clause part of any joins
	 * (optional operation)
	 */
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses);
	/**
	 * Get the from clause part of any joins
	 * (optional operation)
	 */
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses);
	/**
	 * The columns to join on
	 */
	public String[] getKeyColumnNames();
	/**
	 * Get the where clause filter, given a query alias and considering enabled session filters
	 */
	public String filterFragment(String alias, Map enabledFilters) throws MappingException;

	public String oneToManyFilterFragment(String alias) throws MappingException;
	/**
	 * Is this instance actually a CollectionPersister?
	 */
	public boolean isCollection();

	/**
	 * Very, very, very ugly...
	 *
	 * @return Does this persister "consume" entity column aliases in the result
	 * set?
	 */
	public boolean consumesEntityAlias();

	/**
	 * Very, very, very ugly...
	 *
	 * @return Does this persister "consume" collection column aliases in the result
	 * set?
	 */
	public boolean consumesCollectionAlias();
}
