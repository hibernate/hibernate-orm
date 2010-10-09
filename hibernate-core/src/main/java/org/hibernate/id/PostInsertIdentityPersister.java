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
package org.hibernate.id;

import org.hibernate.persister.entity.EntityPersister;

/**
 * A persister that may have an identity assigned by execution of 
 * a SQL <tt>INSERT</tt>.
 *
 * @author Gavin King
 */
public interface PostInsertIdentityPersister extends EntityPersister {
	/**
	 * Get a SQL select string that performs a select based on a unique
	 * key determined by the given property name).
	 *
	 * @param propertyName The name of the property which maps to the
	 * column(s) to use in the select statement restriction.
	 * @return The SQL select string
	 */
	public String getSelectByUniqueKeyString(String propertyName);

	/**
	 * Get the database-specific SQL command to retrieve the last
	 * generated IDENTITY value.
	 *
	 * @return The SQL command string
	 */
	public String getIdentitySelectString();

	/**
	 * The names of the primary key columns in the root table.
	 *
	 * @return The primary key column names.
	 */
	public String[] getRootTableKeyColumnNames();
}
