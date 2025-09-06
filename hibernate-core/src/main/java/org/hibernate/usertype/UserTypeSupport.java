/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import java.util.function.BiConsumer;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

/**
 * @author Steve Ebersole
 */
public class UserTypeSupport<T> extends BaseUserTypeSupport<T> implements TypeConfigurationAware {
	private final Class<T> returnedClass;
	private final int jdbcTypeCode;

	private TypeConfiguration typeConfiguration;

	public UserTypeSupport(Class<T> returnedClass, int jdbcTypeCode) {
		this.returnedClass = returnedClass;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	protected void resolve(BiConsumer<BasicJavaType<T>, JdbcType> resolutionConsumer) {
		assert typeConfiguration != null;
		final var descriptor =
				typeConfiguration.getJavaTypeRegistry()
						.resolveDescriptor( returnedClass );
		resolutionConsumer.accept(
				(BasicJavaType<T>) descriptor,
				typeConfiguration
						.getJdbcTypeRegistry()
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
