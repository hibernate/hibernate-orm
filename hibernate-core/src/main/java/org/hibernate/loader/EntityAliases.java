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
package org.hibernate.loader;
import org.hibernate.persister.entity.Loadable;

/**
 * Metadata describing the SQL result set column aliases
 * for a particular entity.
 * 
 * @author Gavin King
 */
public interface EntityAliases {
	/**
	 * The result set column aliases for the primary key columns
	 */
	public String[] getSuffixedKeyAliases();
	/**
	 * The result set column aliases for the discriminator columns
	 */
	public String getSuffixedDiscriminatorAlias();
	/**
	 * The result set column aliases for the version columns
	 */
	public String[] getSuffixedVersionAliases();
	/**
	 * The result set column aliases for the property columns
	 */
	public String[][] getSuffixedPropertyAliases();
	/**
	 * The result set column aliases for the property columns of a subclass
	 */
	public String[][] getSuffixedPropertyAliases(Loadable persister);
	/**
	 * The result set column alias for the Oracle row id
	 */
	public String getRowIdAlias();

}
