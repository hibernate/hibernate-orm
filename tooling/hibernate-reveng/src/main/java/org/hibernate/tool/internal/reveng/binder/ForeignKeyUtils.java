/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2019-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Collection;
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

    public static List<Object> findForeignKeys(Collection<ForeignKey> foreignKeys, List<Column> pkColumns) {
    	List<ForeignKey> tempList = new ArrayList<ForeignKey>();
     	for (ForeignKey fk : foreignKeys) {
    		tempList.add(fk);
    	}
    	List<Object> result = new ArrayList<Object>();
    	Column[] myPkColumns = pkColumns.toArray(new Column[pkColumns.size()]);
    	for (int i = 0; i < myPkColumns.length; i++) {
    		boolean foundKey = false;
    	    for (ForeignKey key : tempList) {
    			List<Column> matchingColumns = columnMatches(myPkColumns, i, key);
    			if(!matchingColumns.isEmpty()) {
    				result.add(new ForeignKeyForColumns(key, matchingColumns));
    				i+=matchingColumns.size()-1;
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
