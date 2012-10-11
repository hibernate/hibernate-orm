/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.test.util;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * Check that the Hibernate metamodel contains some database objects
 *
 * @author Brett Meyer
 */
public abstract class SchemaUtil {
	
	public static boolean isColumnPresent(
			String tableName, String columnName, Metadata metadata ) {
		try {
			TableSpecification table = getTable( tableName, metadata );
			return ( table.locateColumn( columnName ) == null ) ? false : true;
		} catch ( AssertionFailure e ) {
			return false;
		}
	}

	public static boolean isTablePresent( String tableName, Metadata metadata ) {
		try {
			TableSpecification table = getTable( tableName, metadata );
			return ( table == null ) ? false : true;
		} catch ( AssertionFailure e ) {
			return false;
		}
	}
	
	public static TableSpecification getTable( 
			Class<?> entityClass, Metadata metadata ) throws AssertionFailure {
		final EntityBinding binding = metadata.getEntityBinding( 
				entityClass.getName() );
		return binding.getPrimaryTable();
	}
	
	public static TableSpecification getTable( 
			String tableName, Metadata metadata ) throws AssertionFailure {
		final EntityBinding binding = metadata.getEntityBinding( tableName );
		return binding.locateTable( tableName );
	}
	
	public static Column getColumn( Class<?> entityClass, String columnName,
			Metadata metadata ) throws AssertionFailure {
		return getTable( entityClass, metadata ).locateColumn( columnName );
	}
	
	public static Column getColumn( String tableName, String columnName,
			Metadata metadata ) throws AssertionFailure {
		return getTable( tableName, metadata ).locateColumn( columnName );
	}
	
	public static PrimaryKey getPrimaryKey( Class<?> entityClass,
			Metadata metadata ) throws AssertionFailure {
		return getTable( entityClass, metadata ).getPrimaryKey();
	}
}
