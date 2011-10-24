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
package org.hibernate.mapping;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.JoinedIterator;

/**
 * @author Gavin King
 */
public class DenormalizedTable extends Table {
	
	private final Table includedTable;
	
	public DenormalizedTable(Table includedTable) {
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}
	
	@Override
    public void createForeignKeys() {
		includedTable.createForeignKeys();
		Iterator iter = includedTable.getForeignKeyIterator();
		while ( iter.hasNext() ) {
			ForeignKey fk = (ForeignKey) iter.next();
			createForeignKey( 
					fk.getName() + Integer.toHexString( getName().hashCode() ), 
					fk.getColumns(), 
					fk.getReferencedEntityName() 
				);
		}
	}

	@Override
    public Column getColumn(Column column) {
		Column superColumn = super.getColumn( column );
		if (superColumn != null) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( column );
		}
	}

	@Override
    public Iterator getColumnIterator() {
		return new JoinedIterator(
				includedTable.getColumnIterator(),
				super.getColumnIterator()
			);
	}

	@Override
    public boolean containsColumn(Column column) {
		return super.containsColumn(column) || includedTable.containsColumn(column);
	}

	@Override
    public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}

	@Override
    public Iterator getUniqueKeyIterator() {
		//wierd implementation because of hacky behavior
		//of Table.sqlCreateString() which modifies the
		//list of unique keys by side-effect on some
		//dialects
		Map uks = new HashMap();
		uks.putAll( getUniqueKeys() );
		uks.putAll( includedTable.getUniqueKeys() );
		return uks.values().iterator();
	}

	@Override
    public Iterator getIndexIterator() {
		List indexes = new ArrayList();
		Iterator iter = includedTable.getIndexIterator();
		while ( iter.hasNext() ) {
			Index parentIndex = (Index) iter.next();
			Index index = new Index();
			index.setName( getName() + parentIndex.getName() );
			index.setTable(this);
			index.addColumns( parentIndex.getColumnIterator() );
			indexes.add( index );
		}
		return new JoinedIterator(
				indexes.iterator(),
				super.getIndexIterator()
			);
	}

}
