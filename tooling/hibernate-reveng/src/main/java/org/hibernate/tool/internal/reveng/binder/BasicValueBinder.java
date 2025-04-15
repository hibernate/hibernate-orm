/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.internal.reveng.util.EnhancedBasicValue;

class BasicValueBinder extends AbstractBinder {
	
	static BasicValueBinder create(BinderContext binderContext) {
		return new BasicValueBinder(binderContext);
	}
	
	private BasicValueBinder(BinderContext binderContext) {
		super(binderContext);
	}
	
	EnhancedBasicValue bind(
			Table table,
			Column column,
			boolean generatedIdentifier) {
		EnhancedBasicValue value = new EnhancedBasicValue(getMetadataBuildingContext(), table);
		value.addColumn(column);
		value.setTypeName(TypeUtils.determinePreferredType(
				getMetadataCollector(), 
				getRevengStrategy(),
				table, 
				column, 
				generatedIdentifier));
		if (generatedIdentifier) {
			value.setNullValue("undefined");
		}
		return value;
	}

}
