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
import java.util.Iterator;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;

/**
 * Check that the Hibernate metamodel contains some database objects
 *
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class SchemaUtil {

	private SchemaUtil() {}

	public static boolean isColumnPresent(String tableName, String columnName, MetadataImplementor metadata) {
		try {
			TableSpecification table = getTable( tableName, metadata );
			return ( table.locateColumn( columnName ) != null );
		}
		catch ( AssertionFailure e ) {
			return false;
		}
	}

	public static boolean isTablePresent(String tableName, MetadataImplementor metadata) {
		try {
			TableSpecification table = getTable( tableName, metadata );
			return ( table != null );
		}
		catch ( AssertionFailure e ) {
			return false;
		}
	}

	public static TableSpecification getTable(Class<?> entityClass, MetadataImplementor metadata)
			throws AssertionFailure {
		return metadata.getEntityBinding( entityClass.getName() ).getPrimaryTable();
	}

	public static TableSpecification getTable(String tableName, MetadataImplementor metadata) throws AssertionFailure {
		Iterable<Schema> schemas = metadata.getDatabase().getSchemas();
		for ( Schema schema : schemas ) {
			TableSpecification table = schema.locateTable( Identifier.toIdentifier( tableName ) );
			if ( table != null ) {
				return table;
			}
		}
		throw new AssertionFailure( "can't find table " + tableName );
	}

	public static org.hibernate.metamodel.spi.relational.Column getColumn(Class<?> entityClass, String columnName, MetadataImplementor metadata)
			throws AssertionFailure {
		return getTable( entityClass, metadata ).locateColumn( columnName );
	}

	public static PrimaryKey getPrimaryKey(Class<?> entityClass, MetadataImplementor metadata) throws AssertionFailure {
		return getTable( entityClass, metadata ).getPrimaryKey();
	}

	// TODO: this is not a "schema" method; this should be in MetadataImplmentor
	public static PluralAttributeBinding getCollection(Class<?> entityClass, String fieldName, Metadata metadata) {
		Iterator<PluralAttributeBinding> collectionBindings = metadata.getCollectionBindings().iterator();
		while ( collectionBindings.hasNext() ) {
			PluralAttributeBinding collectionBinding = collectionBindings.next();
			if ( collectionBinding.getAttribute().getName().equals( fieldName ) ) {
				final String collectionOwnerName = collectionBinding.getAttribute()
						.getAttributeContainer()
						.getDescriptor()
						.getName()
						.toString();
				if ( entityClass.getName().equals( collectionOwnerName ) ) {
					return collectionBinding;
				}
			}
		}
		return null;
	}

	public static TableSpecification getCollectionTable(Class<?> entityClass, String fieldName, Metadata metadata) {
		PluralAttributeBinding collection = getCollection( entityClass, fieldName, metadata );
		return collection.getPluralAttributeKeyBinding().getCollectionTable();
	}

	/**
	 * Do all of the given columns have associated UKs?
	 */
	public static boolean columnHasUniqueKey(TableSpecification table, String columnName) {
		return table.hasUniqueKey( table.locateColumn( columnName ) );
	}

	public static boolean hasUniqueKey(TableSpecification table, String keyName) {
		Identifier identifier = Identifier.toIdentifier( keyName );
		for ( UniqueKey uk : table.getUniqueKeys() ) {
			if ( uk.getName().equals( identifier ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Does a unique key exist with the given keyName containing the given columnNames *exclusively*?
	 * 
	 * @param table
	 * @param keyName
	 * @param columnNames
	 * @return
	 */
	public static boolean hasUniqueKey(TableSpecification table, String keyName, String... columnNames) {
		Identifier identifier = Identifier.toIdentifier( keyName );
		for ( UniqueKey uk : table.getUniqueKeys() ) {
			if ( uk.getName().equals( identifier ) ) {
				for (String columnName : columnNames) {
					if (!uk.hasColumn( columnName )) {
						return false;
					}
					return columnNames.length == uk.getColumnSpan();
				}
			}
		}
		return false;
	}

	public static boolean hasForeignKey(TableSpecification table, String keyName) {
		return table.locateForeignKey( keyName ) != null;
	}

	public static boolean hasForeignKey(TableSpecification table, String keyName, String... columnNames) {
		ForeignKey fk = table.locateForeignKey( keyName );
		if (fk == null) {
			return false;
		}
		for (String columnName : columnNames) {
			if (!fk.hasColumn( columnName )) {
				return false;
			}
		}
		return true;
	}

	public static boolean hasIndex(TableSpecification table, String indexName) {
		Identifier identifier = Identifier.toIdentifier( indexName );
		for ( Index index : table.getIndexes() ) {
			if ( index.getName().equals( identifier ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Does an exist with the given name containing the given columnNames *exclusively* & in order?
	 * 
	 * @param table
	 * @param name
	 * @param columnNames
	 * @return
	 */
	public static boolean hasIndex(TableSpecification table, String name, String... columnNames) {
		Identifier identifier = Identifier.toIdentifier( name );
		for ( Index index : table.getIndexes() ) {
			if ( index.getName().equals( identifier ) ) {
				if (columnNames.length != index.getColumnSpan()) {
					return false;
				}
				
				for (int i = 0; i < index.getColumnSpan(); i++) {
					if (! index.getColumns().get( i ).getColumnName().getText().equals( columnNames[i] ) ) {
						return false;
					}
				}
				
				return true;
			}
		}
		return false;
	}
}
