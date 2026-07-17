/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.boot.model.process.internal.NamedBasicTypeResolution;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.internal.core.util.EnhancedBasicValue;
import org.hibernate.type.BasicType;

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
		final TypeUtils.PreferredType preferredType = TypeUtils.determinePreferredTypeDetails(
				getMetadataCollector(),
				getRevengStrategy(),
				table,
				column,
				generatedIdentifier);
		value.setTypeName(preferredType.name());
		applyResolution(value, preferredType.basicType());
		if (generatedIdentifier) {
			value.setNullValue("undefined");
		}
		return value;
	}

	private <J> void applyResolution(EnhancedBasicValue value, BasicType<J> basicType) {
		if ( basicType != null ) {
			value.applyResolution(
					new NamedBasicTypeResolution<>(
							basicType.getJavaTypeDescriptor(),
							basicType,
							basicType.getValueConverter(),
							null
					),
					MappingResolutionState.from( getMetadataBuildingContext() )
			);
		}
	}

}
