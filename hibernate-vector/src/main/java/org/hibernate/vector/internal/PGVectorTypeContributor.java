/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicCollectionType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

public class PGVectorTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		if ( dialect instanceof PostgreSQLDialect ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final ArrayJdbcType genericVectorJdbcType = new PGVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR,
					"vector"
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );
			final ArrayJdbcType floatVectorJdbcType = new PGVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR_FLOAT32,
					"vector"
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );
			final ArrayJdbcType float16VectorJdbcType = new PGVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					SqlTypes.VECTOR_FLOAT16,
					"halfvec"
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT16, float16VectorJdbcType );
			final JdbcType bitVectorJdbcType = new PGBinaryVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT )
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_BINARY, bitVectorJdbcType );
			final JdbcType sparseFloatVectorJdbcType = new PGSparseFloatVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT )
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.SPARSE_VECTOR_FLOAT32, sparseFloatVectorJdbcType );

			javaTypeRegistry.addDescriptor( SparseFloatVectorJavaType.INSTANCE );

			basicTypeRegistry.register(
					new BasicArrayType<>(
							floatBasicType,
							genericVectorJdbcType,
							javaTypeRegistry.getDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							floatVectorJdbcType,
							javaTypeRegistry.getDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT32.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							float16VectorJdbcType,
							javaTypeRegistry.getDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT16.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.BYTE ),
							bitVectorJdbcType,
							javaTypeRegistry.getDescriptor( byte[].class )
					),
					StandardBasicTypes.VECTOR_BINARY.getName()
			);
			basicTypeRegistry.register(
					new BasicCollectionType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							sparseFloatVectorJdbcType,
							SparseFloatVectorJavaType.INSTANCE,
							"sparse_float_vector"
					)
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR, "vector($l)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT32, "vector($l)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_BINARY, "bit($l)", "bit", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT16, "halfvec($l)", "halfvec", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.SPARSE_VECTOR_FLOAT32, "sparsevec($l)", "sparsevec", dialect )
			);
		}
	}
}
