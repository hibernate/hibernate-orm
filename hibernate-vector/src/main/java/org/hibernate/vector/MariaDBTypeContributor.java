/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import java.lang.reflect.Type;

public class MariaDBTypeContributor implements TypeContributor {

	private static final Type[] VECTOR_JAVA_TYPES = {
			Float[].class,
			float[].class
	};

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		if ( dialect instanceof MariaDBDialect ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final ArrayJdbcType vectorJdbcType = new BinaryVectorJdbcType( jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ) );
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, vectorJdbcType );
			for ( Type vectorJavaType : VECTOR_JAVA_TYPES ) {
				basicTypeRegistry.register(
						new BasicArrayType<>(
								floatBasicType,
								vectorJdbcType,
								javaTypeRegistry.getDescriptor( vectorJavaType )
						),
						StandardBasicTypes.VECTOR.getName()
				);
			}
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new DdlTypeImpl( SqlTypes.VECTOR, "vector($l)", "vector", dialect ) {
						@Override
						public String getTypeName(Size size) {
							return getTypeName(
									size.getArrayLength() == null ? null : size.getArrayLength().longValue(),
									null,
									null
							);
						}
					}
			);
		}
	}
}
