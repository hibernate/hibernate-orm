/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.enums;

import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.type.EnumType;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class TestEnumTypeSerialization {
	@Test
	public void testOrdinalSerializability() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		final EnumJavaTypeDescriptor<UnspecifiedEnumTypeEntity.E1> enumJtd = (EnumJavaTypeDescriptor) typeConfiguration
				.getJavaTypeDescriptorRegistry()
				.resolveDescriptor( UnspecifiedEnumTypeEntity.E1.class );

		final OrdinalEnumValueConverter valueConverter = new OrdinalEnumValueConverter(
				enumJtd,
				IntegerJdbcType.INSTANCE,
				IntegerJavaTypeDescriptor.INSTANCE
		);

		final EnumType<UnspecifiedEnumTypeEntity.E1> enumType = new EnumType<>(
				UnspecifiedEnumTypeEntity.E1.class,
				valueConverter,
				typeConfiguration
		);

		SerializationHelper.clone( enumType );
	}

	@Test
	public void testNamedSerializability() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		final EnumJavaTypeDescriptor<UnspecifiedEnumTypeEntity.E1> enumJtd = (EnumJavaTypeDescriptor) typeConfiguration
				.getJavaTypeDescriptorRegistry()
				.resolveDescriptor( UnspecifiedEnumTypeEntity.E1.class );

		final NamedEnumValueConverter valueConverter = new NamedEnumValueConverter(
				enumJtd,
				VarcharJdbcType.INSTANCE,
				StringJavaTypeDescriptor.INSTANCE
		);

		final EnumType<UnspecifiedEnumTypeEntity.E1> enumType = new EnumType<>(
				UnspecifiedEnumTypeEntity.E1.class,
				valueConverter,
				typeConfiguration
		);

		SerializationHelper.clone( enumType );
	}
}
