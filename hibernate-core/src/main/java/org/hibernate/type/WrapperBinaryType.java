/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.sql.Types;

import org.hibernate.type.descriptor.java.ByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.VarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type mapping {@link java.sql.Types#VARBINARY VARBINARY} and {@link Byte Byte[]}
 * 
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class WrapperBinaryType extends AbstractSingleColumnStandardBasicType<Byte[]> implements AdjustableBasicType<Byte[]> {
	public static final WrapperBinaryType INSTANCE = new WrapperBinaryType();

	public WrapperBinaryType() {
		super( VarbinaryTypeDescriptor.INSTANCE, ByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "Byte[]", Byte[].class.getName() };
	}

	public String getName() {
		//TODO find a decent name before documenting
		return "wrapper-binary";
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		if ( ! indicators.isLob() ) {
			//noinspection unchecked
			return (BasicType<X>) this;
		}

		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
		return typeConfiguration.getBasicTypeRegistry().resolve(
				domainJtd,
				jdbcTypeRegistry.getDescriptor( Types.BLOB ),
				getName()
		);
	}
}
