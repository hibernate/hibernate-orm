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

import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

class ManyToOneBinder extends AbstractBinder {
	
	static ManyToOneBinder create(BinderContext binderContext) {
		return new ManyToOneBinder(binderContext);
	}
	
	private final EntityPropertyBinder entityPropertyBinder;
	
	private ManyToOneBinder(BinderContext binderContext) {
		super(binderContext);
		this.entityPropertyBinder = EntityPropertyBinder.create(binderContext);
	}

    Property bind(
    		String propertyName, 
    		boolean mutable, 
    		Table table, 
    		ForeignKey fk, 
    		Set<Column> processedColumns) {   	
        ManyToOne value = new ManyToOne(getMetadataBuildingContext(), table);
        value.setReferencedEntityName( fk.getReferencedEntityName() );
		addColumns(value, fk, processedColumns);
        value.setFetchMode(FetchMode.SELECT);
        return entityPropertyBinder
        		.bind(
        				propertyName, 
        				mutable, 
        				table, 
        				fk, 
        				value, 
        				false);
     }
    
    private void addColumns(
    		ManyToOne value,
    		ForeignKey fk, 
    		Set<Column> processedColumns) {
		for (Column fkcolumn : fk.getColumns()) {
			BinderUtils.checkColumnForMultipleBinding(fkcolumn);
            value.addColumn(fkcolumn);
            processedColumns.add(fkcolumn);
		}
    }

}
