/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicCollectionType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleVectorTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();

		if ( dialect instanceof OracleDialect && dialect.getVersion().isSameOrAfter( 23, 4 ) ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

			final boolean isVectorSupported = isVectorSupportedByDriver( (OracleDialect) dialect );

			// Register generic vector type
			final OracleVectorJdbcType genericVectorJdbcType = new OracleVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );
			final JdbcType floatVectorJdbcType = new OracleFloatVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );
			final JdbcType doubleVectorJdbcType = new OracleDoubleVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.DOUBLE ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT64, doubleVectorJdbcType );
			final JdbcType byteVectorJdbcType = new OracleByteVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_INT8, byteVectorJdbcType );
			final JdbcType bitVectorJdbcType = new OracleBinaryVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_BINARY, bitVectorJdbcType );
			final JdbcType sparseByteVectorJdbcType = new OracleSparseByteVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.SPARSE_VECTOR_INT8, sparseByteVectorJdbcType );
			final JdbcType sparseFloatVectorJdbcType = new OracleSparseFloatVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.SPARSE_VECTOR_FLOAT32, sparseFloatVectorJdbcType );
			final JdbcType sparseDoubleVectorJdbcType = new OracleSparseDoubleVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.DOUBLE ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.SPARSE_VECTOR_FLOAT64, sparseDoubleVectorJdbcType );

			javaTypeRegistry.addDescriptor( SparseByteVectorJavaType.INSTANCE );
			javaTypeRegistry.addDescriptor( SparseFloatVectorJavaType.INSTANCE );
			javaTypeRegistry.addDescriptor( SparseDoubleVectorJavaType.INSTANCE );

			// Resolving basic types  after jdbc types are registered.
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							genericVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							floatVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT32.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ),
							doubleVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( double[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT64.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.BYTE ),
							byteVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( byte[].class )
					),
					StandardBasicTypes.VECTOR_INT8.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.BYTE ),
							bitVectorJdbcType,
							javaTypeRegistry.resolveDescriptor( byte[].class )
					),
					StandardBasicTypes.VECTOR_BINARY.getName()
			);
			basicTypeRegistry.register(
					new BasicCollectionType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.BYTE ),
							sparseByteVectorJdbcType,
							SparseByteVectorJavaType.INSTANCE,
							"sparse_byte_vector"
					)
			);
			basicTypeRegistry.register(
					new BasicCollectionType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							sparseFloatVectorJdbcType,
							SparseFloatVectorJavaType.INSTANCE,
							"sparse_float_vector"
					)
			);
			basicTypeRegistry.register(
					new BasicCollectionType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ),
							sparseDoubleVectorJdbcType,
							SparseDoubleVectorJavaType.INSTANCE,
							"sparse_double_vector"
					)
			);

			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR, "vector($l,*)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_INT8, "vector($l,int8)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT32, "vector($l,float32)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT64, "vector($l,float64)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_BINARY, "vector($l,binary)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.SPARSE_VECTOR_INT8, "vector($l,int8,sparse)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.SPARSE_VECTOR_FLOAT32, "vector($l,float32,sparse)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.SPARSE_VECTOR_FLOAT64, "vector($l,float64,sparse)", "vector", dialect )
			);
		}
	}

	private boolean isVectorSupportedByDriver(OracleDialect dialect) {
		int majorVersion = dialect.getDriverMajorVersion();
		int minorVersion = dialect.getDriverMinorVersion();

		return ( majorVersion > 23 ) || ( majorVersion == 23 && minorVersion >= 4 );
	}
}
