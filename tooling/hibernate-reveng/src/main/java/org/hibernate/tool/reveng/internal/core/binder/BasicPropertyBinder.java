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
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.internal.core.util.RevengUtils;

class BasicPropertyBinder extends AbstractBinder {
	
	static BasicPropertyBinder create(BinderContext binderContext) {
		return new BasicPropertyBinder(binderContext);
	}
	
	private final BasicValueBinder simpleValueBinder;
	private final PropertyBinder propertyBinder;
	
	private BasicPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		simpleValueBinder = BasicValueBinder.create(binderContext);
		propertyBinder = PropertyBinder.create(binderContext);
	}
	

	Property bind(String propertyName, Table table, Column column) {
		return propertyBinder.bind(
				table, 
				propertyName, 
				simpleValueBinder.bind(table, column, false), 
				RevengUtils.createAssociationInfo(null, null, true, true));
	}

}
