/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.lang.reflect.Constructor;
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
public final class PgJdbcHelper {

	public static boolean isUsable(ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			classLoaderService.classForName( "org.postgresql.util.PGobject" );
			return true;
		}
		catch (ClassLoadingException ex) {
			return false;
		}
	}

	public static JdbcType getStructJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcType( serviceRegistry, "org.hibernate.dialect.PostgreSQLStructPGObjectJdbcType" );
	}

	public static JdbcType getIntervalJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcType( serviceRegistry, "org.hibernate.dialect.PostgreSQLIntervalSecondJdbcType" );
	}

	public static JdbcType getInetJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcType( serviceRegistry, "org.hibernate.dialect.PostgreSQLInetJdbcType" );
	}

	public static JdbcType getJsonJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcType( serviceRegistry, "org.hibernate.dialect.PostgreSQLJsonPGObjectJsonType" );
	}

	public static JdbcType getJsonbJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcType( serviceRegistry, "org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType" );
	}

	public static JdbcTypeConstructor getJsonArrayJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcTypeConstructor( serviceRegistry, "org.hibernate.dialect.PostgreSQLJsonArrayPGObjectJsonJdbcTypeConstructor" );
	}

	public static JdbcTypeConstructor getJsonbArrayJdbcType(ServiceRegistry serviceRegistry) {
		return createJdbcTypeConstructor( serviceRegistry, "org.hibernate.dialect.PostgreSQLJsonArrayPGObjectJsonbJdbcTypeConstructor" );
	}

	public static JdbcType createJdbcType(ServiceRegistry serviceRegistry, String className) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			final Class<?> clazz = classLoaderService.classForName( className );
			final Constructor<?> constructor = clazz.getConstructor();
			return (JdbcType) constructor.newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new HibernateError( "Class does not have an empty constructor", e );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new HibernateError( "Could not construct JdbcType", e );
		}
	}

	public static JdbcTypeConstructor createJdbcTypeConstructor(ServiceRegistry serviceRegistry, String className) {
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			final Class<?> clazz = classLoaderService.classForName( className );
			final Constructor<?> constructor = clazz.getConstructor();
			return (JdbcTypeConstructor) constructor.newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new HibernateError( "Class does not have an empty constructor", e );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new HibernateError( "Could not construct JdbcTypeConstructor", e );
		}
	}
}
