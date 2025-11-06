/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.internal.core.util.EnhancedBasicValue;

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
