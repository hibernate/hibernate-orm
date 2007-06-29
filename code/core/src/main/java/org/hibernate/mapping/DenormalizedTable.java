//$Id: DenormalizedTable.java 10880 2006-11-29 15:18:34Z epbernard $
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.util.JoinedIterator;

/**
 * @author Gavin King
 */
public class DenormalizedTable extends Table {
	
	private final Table includedTable;
	
	public DenormalizedTable(Table includedTable) {
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}
	
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

	public Column getColumn(Column column) {
		Column superColumn = super.getColumn( column );
		if (superColumn != null) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( column );
		}
	}

	public Iterator getColumnIterator() {
		return new JoinedIterator(
				includedTable.getColumnIterator(),
				super.getColumnIterator()
			);
	}

	public boolean containsColumn(Column column) {
		return super.containsColumn(column) || includedTable.containsColumn(column);
	}

	public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}

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
