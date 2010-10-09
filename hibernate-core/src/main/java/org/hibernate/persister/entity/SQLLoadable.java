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

import org.hibernate.type.Type;

/**
 * A class persister that supports queries expressed in the
 * platform native SQL dialect
 *
 * @author Gavin King, Max Andersen
 */
public interface SQLLoadable extends Loadable {

	/**
	 * Return the column alias names used to persist/query the named property of the class or a subclass (optional operation).
	 */
	public String[] getSubclassPropertyColumnAliases(String propertyName, String suffix);

	/**
	 * Return the column names used to persist/query the named property of the class or a subclass (optional operation).
	 */
	public String[] getSubclassPropertyColumnNames(String propertyName);
	
	/**
	 * All columns to select, when loading.
	 */
	public String selectFragment(String alias, String suffix);

	/**
	 * Get the type
	 */
	public Type getType();

	

}
