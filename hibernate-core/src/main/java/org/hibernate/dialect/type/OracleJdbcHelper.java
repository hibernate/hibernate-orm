/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateError;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

/**
 * The following class provides some convenience methods for accessing JdbcType instance,
 * that are loaded into the app class loader, where they have access to the JDBC driver classes.
 *
 * @author Christian Beikov
 */
public class OracleJdbcHelper {

	public static boolean isUsable(ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			classLoaderService.classForName( "oracle.jdbc.OracleConnection" );
			return true;
		}
		catch (ClassLoadingException ex) {
			return false;
		}
	}
	public static boolean isOsonAvailable(ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			classLoaderService.classForName( "oracle.sql.json.OracleJsonFactory" );
			return true;
		}
		catch (ClassLoadingException ex) {
			return false;
		}
	}

	public static JdbcTypeConstructor getArrayJdbcTypeConstructor(ServiceRegistry serviceRegistry) {
		return create( serviceRegistry, "org.hibernate.dialect.type.OracleArrayJdbcTypeConstructor" );
	}

	public static JdbcTypeConstructor getNestedTableJdbcTypeConstructor(ServiceRegistry serviceRegistry) {
		return create( serviceRegistry, "org.hibernate.dialect.type.OracleNestedTableJdbcTypeConstructor" );
	}

	public static JdbcType getStructJdbcType(ServiceRegistry serviceRegistry) {
		return create( serviceRegistry, "org.hibernate.dialect.type.OracleStructJdbcType" );
	}

	private static <X> X create(ServiceRegistry serviceRegistry, String className) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			return classLoaderService.<X>classForName( className ).getConstructor().newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new HibernateError( "Class does not have an empty constructor", e );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new HibernateError( "Could not construct JdbcType", e );
		}
	}
}
