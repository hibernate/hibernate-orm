/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
