/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.HibernateError;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
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

import java.lang.reflect.InvocationTargetException;

public class SQLServerTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		if ( dialect instanceof SQLServerDialect && dialect.getVersion().isSameOrAfter( 17 ) ) {
			final boolean supportsDriverType = supportsDriverType( serviceRegistry );
			final String vectorJdbcType = supportsDriverType ? "org.hibernate.vector.internal.SQLServerVectorJdbcType"
					: "org.hibernate.vector.internal.SQLServerCastingVectorJdbcType";
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final JdbcType floatJdbcType = jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT );
			final ArrayJdbcType genericVectorJdbcType = create(
					serviceRegistry,
					vectorJdbcType,
					floatJdbcType,
					SqlTypes.VECTOR
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );
			final ArrayJdbcType floatVectorJdbcType = create(
					serviceRegistry,
					vectorJdbcType,
					floatJdbcType,
					SqlTypes.VECTOR_FLOAT32
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );
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
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR, "vector($l)", "vector", dialect )
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new VectorDdlType( SqlTypes.VECTOR_FLOAT32, "vector($l)", "vector", dialect )
			);
		}
	}

	private static boolean supportsDriverType(ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			classLoaderService.classForName( "microsoft.sql.Vector" );
			return true;
		}
		catch (ClassLoadingException ex) {
			return false;
		}
	}

	private static <X> X create(ServiceRegistry serviceRegistry, String className, JdbcType elementType, int sqlType) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			return classLoaderService.<X>classForName( className )
					.getConstructor( JdbcType.class, int.class )
					.newInstance( elementType, sqlType );
		}
		catch (NoSuchMethodException e) {
			throw new HibernateError( "Class does not have an empty constructor", e );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new HibernateError( "Could not construct JdbcType", e );
		}
	}
}
