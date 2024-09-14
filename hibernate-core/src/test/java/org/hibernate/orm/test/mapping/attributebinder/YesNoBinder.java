/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.attributebinder;

import org.hibernate.boot.model.convert.internal.InstanceBasedConverterDescriptor;
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
				new InstanceBasedConverterDescriptor(
						YesNoConverter.INSTANCE,
						buildingContext.getBootstrapContext().getClassmateContext()
				)
		);
	}
}
//end::attribute-binder-example[]
