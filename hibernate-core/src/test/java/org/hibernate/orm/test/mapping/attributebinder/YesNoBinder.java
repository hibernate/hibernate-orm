/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.sql.Types;

import org.hibernate.boot.model.convert.internal.InstanceBasedConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tuple.AttributeBinder;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

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
		final BasicValue booleanValueMapping = (BasicValue) property.getValue();

		final BasicJavaType<?> javaType = (BasicJavaType<?>) buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( Boolean.class );

		final JdbcType jdbcType = buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry()
				.getDescriptor( Types.CHAR );

		final InstanceBasedConverterDescriptor converter = new InstanceBasedConverterDescriptor(
				YesNoConverter.INSTANCE,
				buildingContext.getBootstrapContext().getClassmateContext()
		);

		booleanValueMapping.setExplicitJavaTypeAccess( (typeConfiguration) -> javaType );
		booleanValueMapping.setExplicitJdbcTypeAccess( (typeConfiguration) -> jdbcType );
		booleanValueMapping.setJpaAttributeConverterDescriptor( converter );
	}
}
//end::attribute-binder-example[]

