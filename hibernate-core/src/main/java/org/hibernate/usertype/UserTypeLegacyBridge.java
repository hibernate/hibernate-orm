/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import java.util.Properties;
import java.util.function.BiConsumer;

import org.hibernate.MappingException;
import org.hibernate.annotations.Type;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

/**
 * Convenience {@link UserType} implementation which mimics the legacy <code>@Type</code>
 * annotation which was based on the {@code hbm.xml} mapping's string-based type support.
 *
 * @see Type
 */
public class UserTypeLegacyBridge extends BaseUserTypeSupport<Object> implements ParameterizedType, TypeConfigurationAware {
	public static final String TYPE_NAME_PARAM_KEY = "hbm-type-name";

	private TypeConfiguration typeConfiguration;
	private String hbmStyleTypeName;

	public UserTypeLegacyBridge() {
	}

	public UserTypeLegacyBridge(String hbmStyleTypeName) {
		this.hbmStyleTypeName = hbmStyleTypeName;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		if ( hbmStyleTypeName == null ) {
			hbmStyleTypeName = parameters.getProperty( TYPE_NAME_PARAM_KEY );
			if ( hbmStyleTypeName == null ) {
				throw new MappingException( "Missing `@Parameter` for `" + TYPE_NAME_PARAM_KEY + "`" );
			}
		}
		// otherwise assume it was ctor-injected
	}

	@Override
	protected void resolve(BiConsumer<BasicJavaType<Object>, JdbcType> resolutionConsumer) {
		assert typeConfiguration != null;

		final BasicType<Object> registeredType =
						typeConfiguration.getBasicTypeRegistry()
								.getRegisteredType( hbmStyleTypeName );

		resolutionConsumer.accept(
				(BasicJavaType<Object>)
						registeredType.getJavaTypeDescriptor(),
				registeredType.getJdbcType()
		);
	}
}
