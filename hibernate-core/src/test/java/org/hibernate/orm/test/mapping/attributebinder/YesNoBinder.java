/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder;

import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.AttributeBindingContext;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.YesNoConverter;

//tag::attribute-binder-example[]
/**
 * The actual binder responsible for configuring the model objects
 */
public class YesNoBinder implements AttributeBinder<YesNo> {
	@Override
	public void bind(YesNo annotation, AttributeBindingContext context) {
		( (SimpleValue) context.getProperty().getValue() ).setJpaAttributeConverterDescriptor(
				ConverterDescriptors.of( YesNoConverter.INSTANCE )
		);
	}
}
//end::attribute-binder-example[]
