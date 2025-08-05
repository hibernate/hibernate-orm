/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
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
		if ( dialect instanceof MariaDBDialect && dialect.getVersion().isSameOrAfter( 11, 7 ) ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final ArrayJdbcType genericVectorJdbcType = new MariaDBVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );
			final ArrayJdbcType floatVectorJdbcType = new MariaDBVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR_FLOAT32
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );
			for ( Type vectorJavaType : VECTOR_JAVA_TYPES ) {
				basicTypeRegistry.register(
						new BasicArrayType<>(
								floatBasicType,
								genericVectorJdbcType,
								javaTypeRegistry.getDescriptor( vectorJavaType )
						),
						StandardBasicTypes.VECTOR.getName()
				);
				basicTypeRegistry.register(
						new BasicArrayType<>(
								basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
								floatVectorJdbcType,
								javaTypeRegistry.getDescriptor( vectorJavaType )
						),
						StandardBasicTypes.VECTOR_FLOAT32.getName()
				);
			}
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR, "vector($l)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT32, "vector($l)", "vector", dialect )
			);
		}
	}
}
