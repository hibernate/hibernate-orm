/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2015-2025 Red Hat, Inc.
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

import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Fetchable;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.util.RevengUtils;

class PropertyBinder extends AbstractBinder {

	static PropertyBinder create(BinderContext binderContext) {
		return new PropertyBinder(binderContext);
	}

	private PropertyBinder(BinderContext binderContext) {
		super(binderContext);
	}
	
	Property bind(
			Table table,
			String propertyName,
			Value value,
			AssociationInfo associationInfo) {
		return bindMetaAttributes(
				createProperty(propertyName, value, associationInfo), 
				table);
	}
	
	private Property createProperty(
			String propertyName, 
			Value value, 
			AssociationInfo associationInfo) {
		Property result = new Property();
		result.setName(propertyName);
		result.setValue(value);
		result.setInsertable(associationInfo.getInsert());
		result.setUpdateable(associationInfo.getUpdate());
		String cascade = associationInfo.getCascade();
		cascade = cascade == null ? "none" : cascade;
		result.setCascade(cascade);
		boolean lazy = false;
		if (Fetchable.class.isInstance(value)) {
			lazy = ((Fetchable)value).getFetchMode() != FetchMode.JOIN;
		}
		result.setLazy(lazy);
		result.setPropertyAccessorName("property");
		return result;
	}

    private Property bindMetaAttributes(Property property, Table table) {
		for (Column col : property.getColumns()) {
			Map<String,MetaAttribute> map = getColumnToMetaAttributesInRevengStrategy(table, col.getName());
			if(map!=null) { 
				property.setMetaAttributes(map);
			}
		}

		return property;
    }

	private Map<String,MetaAttribute> getColumnToMetaAttributesInRevengStrategy(
			Table table,
			String column) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = getRevengStrategy().columnToMetaAttributes(tableIdentifier, column);
		if (result == null) {
			tableIdentifier = RevengUtils.createTableIdentifier(
					table, 
					getDefaultCatalog(), 
					getDefaultSchema());
			result = getRevengStrategy().columnToMetaAttributes(tableIdentifier, column);
		}
		return result;
	}
	
}
