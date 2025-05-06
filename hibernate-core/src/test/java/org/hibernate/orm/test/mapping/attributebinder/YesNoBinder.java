/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder;

import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.YesNoConverter;

//tag::attribute-binder-example[]
/**
 * The actual binder responsible for configuring the model objects
 */
public class YesNoBinder implements AttributeBinder<YesNo> {
	@Override
	public void bind(
			YesNo annotation,
			MetadataBuildingContext buildingContext,
			PersistentClass persistentClass,
			Property property) {
		( (SimpleValue) property.getValue() ).setJpaAttributeConverterDescriptor(
				ConverterDescriptors.of(
						YesNoConverter.INSTANCE,
						buildingContext.getBootstrapContext().getClassmateContext()
				)
		);
	}
}
//end::attribute-binder-example[]
