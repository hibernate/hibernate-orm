package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;

public class ForeignKeyUtils {

    public static List<Column> columnMatches(
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

}
