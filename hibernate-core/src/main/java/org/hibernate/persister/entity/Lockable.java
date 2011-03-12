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

/**
 * Contract for things that can be locked via a {@link org.hibernate.dialect.lock.LockingStrategy}.
 * <p/>
 * Currently only the root table gets locked, except for the case of HQL and Criteria queries
 * against dialects which do not support either (1) FOR UPDATE OF or (2) support hint locking
 * (in which case *all* queried tables would be locked).
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public interface Lockable extends EntityPersister {
	/**
	 * Locks are always applied to the "root table".
	 *
	 * @return The root table name
	 */
	public String getRootTableName();

	/**
	 * Get the SQL alias this persister would use for the root table
	 * given the passed driving alias.
	 *
	 * @param drivingAlias The driving alias; or the alias for the table
	 * mapped by this persister in the hierarchy.
	 * @return The root table alias.
	 */
	public String getRootTableAlias(String drivingAlias);

	/**
	 * Get the names of columns on the root table used to persist the identifier.
	 *
	 * @return The root table identifier column names.
	 */
	public String[] getRootTableIdentifierColumnNames();

	/**
	 * For versioned entities, get the name of the column (again, expected on the
	 * root table) used to store the version values.
	 *
	 * @return The version column name.
	 */
	public String getVersionColumnName();
}
