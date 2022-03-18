package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;

public class ForeignKeyUtils {

    public static boolean isUniqueReference(ForeignKey foreignKey) {
		for (ForeignKey element : foreignKey.getTable().getForeignKeys().values()) {
			if(element!=foreignKey && element.getReferencedTable().equals(foreignKey.getReferencedTable())) {
				return false;
			}
		}
		return true;
	}

    public static List<Object> findForeignKeys(Iterator<?> foreignKeyIterator, List<Column> pkColumns) {
    	List<ForeignKey> tempList = new ArrayList<ForeignKey>();
    	while(foreignKeyIterator.hasNext()) {
    		tempList.add((ForeignKey)foreignKeyIterator.next());
    	}
    	List<Object> result = new ArrayList<Object>();
    	Column[] myPkColumns = pkColumns.toArray(new Column[pkColumns.size()]);
    	for (int i = 0; i < myPkColumns.length; i++) {
    		boolean foundKey = false;
    		foreignKeyIterator = tempList.iterator();
    		while(foreignKeyIterator.hasNext()) {
    			ForeignKey key = (ForeignKey)foreignKeyIterator.next();
    			List<Column> matchingColumns = columnMatches(myPkColumns, i, key);
    			if(!matchingColumns.isEmpty()) {
    				result.add(new ForeignKeyForColumns(key, matchingColumns));
    				i+=matchingColumns.size()-1;
    				foreignKeyIterator.remove();
    				foundKey=true;
    				break;
    			}
    		}
    		if(!foundKey) {
    			result.add(myPkColumns[i]);
    		}
		}
    	return result;
    }

    private static List<Column> columnMatches(
    		Column[] pkColumns, 
    		int offset, 
    		ForeignKey fk) {
    	List<Column> result = new ArrayList<Column>();
    	int columnSpan = fk.getColumnSpan();
    	if (columnSpan <= pkColumns.length-offset) {
    		for (int i = 0; i < columnSpan; i++) {
    			Column column = pkColumns[i + offset];
    			if(column.equals(fk.getColumn(i))) {
    				result.add(column);
    			} else {
    				result.clear();
    				break;
    			}
			}
		}
		return result;
	}

	public static class ForeignKeyForColumns {
        public final List<Column> columns;
        public final ForeignKey key;
        public ForeignKeyForColumns(ForeignKey key, List<Column> columns) {
            this.key = key;
            this.columns = columns;
        }
    }
}
