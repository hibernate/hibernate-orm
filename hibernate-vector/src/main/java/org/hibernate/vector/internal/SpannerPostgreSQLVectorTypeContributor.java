/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
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

/**
 * TypeContributor for Cloud Spanner PostgreSQL vector types.
 *
 * @since 7.2
 */
public class SpannerPostgreSQLVectorTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		if ( dialect instanceof SpannerPostgreSQLDialect ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final BasicType<Double> doubleBasicType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );

			final ArrayJdbcType genericVectorJdbcType = new SpannerPostgreSQLVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR,
					"float4"
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );

			final ArrayJdbcType floatVectorJdbcType = new SpannerPostgreSQLVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR_FLOAT32,
					"float4"
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );

			final ArrayJdbcType doubleVectorJdbcType = new SpannerPostgreSQLVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.DOUBLE ),
					SqlTypes.VECTOR_FLOAT64,
					"float8"
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT64, doubleVectorJdbcType );

			basicTypeRegistry.register(
					new BasicArrayType<>(
							floatBasicType,
							genericVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR.getName()
			);

			basicTypeRegistry.register(
					new BasicArrayType<>(
							floatBasicType,
							floatVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT32.getName()
			);

			basicTypeRegistry.register(
					new BasicArrayType<>(
							doubleBasicType,
							doubleVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( double[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT64.getName()
			);

			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new SpannerPostgreSQLVectorDdlType( SqlTypes.VECTOR, "float4", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new SpannerPostgreSQLVectorDdlType( SqlTypes.VECTOR_FLOAT32, "float4", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new SpannerPostgreSQLVectorDdlType( SqlTypes.VECTOR_FLOAT64, "float8", dialect )
			);
		}
	}
}
