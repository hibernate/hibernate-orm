/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.usertype;

import java.util.function.BiConsumer;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

/**
 * @author Steve Ebersole
 */
public class UserTypeSupport<T> extends BaseUserTypeSupport<T> implements TypeConfigurationAware {
	private final Class<?> returnedClass;
	private final int jdbcTypeCode;

	private TypeConfiguration typeConfiguration;

	public UserTypeSupport(Class<?> returnedClass, int jdbcTypeCode) {
		this.returnedClass = returnedClass;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	protected void resolve(BiConsumer<BasicJavaDescriptor<T>, JdbcTypeDescriptor> resolutionConsumer) {
		assert typeConfiguration != null;

		resolutionConsumer.accept(
				(BasicJavaDescriptor<T>) typeConfiguration
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( returnedClass ),
				typeConfiguration
						.getJdbcTypeDescriptorRegistry()
						.getDescriptor( jdbcTypeCode )
		);
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}
}
