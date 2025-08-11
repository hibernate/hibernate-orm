/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

public class DB2VectorTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		if ( dialect instanceof DB2Dialect db2Dialect && db2Dialect.getDB2Version().isSameOrAfter( 12, 1, 2 ) ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final ArrayJdbcType genericVectorJdbcType = new DB2VectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT )
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );
			final ArrayJdbcType floatVectorJdbcType = new DB2FloatVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT )
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );
			final JdbcType byteVectorJdbcType = new DB2ByteVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT )
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_INT8, byteVectorJdbcType );
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
							basicTypeRegistry.resolve( StandardBasicTypes.BYTE ),
							byteVectorJdbcType,
							javaTypeRegistry.getDescriptor( byte[].class )
					),
					StandardBasicTypes.VECTOR_INT8.getName()
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR, "vector($l,float32)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_INT8, "vector($l,int8)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT32, "vector($l,float32)", "vector", dialect )
			);
		}
	}
}
