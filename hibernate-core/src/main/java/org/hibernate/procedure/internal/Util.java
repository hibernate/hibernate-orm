/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.UnknownSqlResultSetMappingException;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.spi.ResultSetMapping;

import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;
import static org.hibernate.query.results.spi.ResultSetMapping.resolveResultSetMapping;

/**
 * Utilities used to implement procedure call support.
 *
 * @author Steve Ebersole
 */
public class Util {

	private Util() {
	}

	public static List<ResultSetMapping> resolveResultSetMappings(
			String procedureName,
			String[] resultSetMappingNames,
			Class<?>[] resultSetMappingClasses,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		if ( !isEmpty( resultSetMappingNames ) ) {
			// cannot specify both
			if ( !isEmpty( resultSetMappingClasses ) ) {
				throw new IllegalArgumentException( "Cannot specify both result-set mapping names and classes" );
			}
			return resolveResultSetMappings( procedureName, resultSetMappingNames, querySpaceConsumer, context );
		}
		else if ( !isEmpty( resultSetMappingClasses ) ) {
			return resolveResultSetMappings( procedureName, resultSetMappingClasses, querySpaceConsumer, context );
		}
		else {
			return List.of( resolveResultSetMapping( procedureName, true, context.getSessionFactory() ) );
		}
	}

	public static void resolveResultSetMappings(
			String[] resultSetMappingNames,
			Class<?>[] resultSetMappingClasses,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		if ( !isEmpty( resultSetMappingNames ) ) {
			// cannot specify both
			if ( !isEmpty( resultSetMappingClasses ) ) {
				throw new IllegalArgumentException( "Cannot specify both result-set mapping names and classes" );
			}
			resolveResultSetMappingNames( resultSetMappingNames, resultSetMapping, querySpaceConsumer, context );
		}
		else if ( !isEmpty( resultSetMappingClasses ) ) {
			resolveResultSetMappingClasses( resultSetMappingClasses, resultSetMapping, querySpaceConsumer, context );
		}

		// otherwise, nothing to resolve
	}

	public static void resolveResultSetMappingNames(
			String[] resultSetMappingNames,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final var namedObjectRepository = context.getNamedObjectRepository();
		for ( String resultSetMappingName : resultSetMappingNames ) {
			final var memento =
					namedObjectRepository.getResultSetMappingMemento( resultSetMappingName );
			if ( memento == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}
			memento.resolve( resultSetMapping, querySpaceConsumer, context );
		}
	}

	public static List<ResultSetMapping> resolveResultSetMappings(
			String procedureName,
			String[] resultSetMappingNames,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final var namedObjectRepository = context.getNamedObjectRepository();
		final var sessionFactory = context.getSessionFactory();
		final List<ResultSetMapping> resultMappings = CollectionHelper.arrayList( resultSetMappingNames.length );
		for ( int i = 0; i < resultSetMappingNames.length; i++ ) {
			final var mappingName = resultSetMappingNames[i];
			final ResultSetMapping mapping = sessionFactory
					.getJdbcValuesMappingProducerProvider()
					.buildResultSetMapping( procedureName + ":" + mappingName, false, sessionFactory );
			final var memento = namedObjectRepository.getResultSetMappingMemento( mappingName );
			if ( memento == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + mappingName + "]" );
			}
			memento.resolve( mapping, querySpaceConsumer, context );
			resultMappings.add( mapping );
		}
		return resultMappings;
	}

	public static ResultSetMapping makeResultSetMapping(
			String procedureName,
			Class<?> resultClass,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final var sessionFactory = context.getSessionFactory();
		final ResultSetMapping clazzMapping = sessionFactory
				.getJdbcValuesMappingProducerProvider()
				.buildResultSetMapping( procedureName + ":" + resultClass.getName(), false, sessionFactory );
		applyResultClass( resultClass, clazzMapping, querySpaceConsumer, context );
		return clazzMapping;
	}

	public static List<ResultSetMapping> resolveResultSetMappings(
			String procedureName,
			Class<?>[] resultSetMappingClasses,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final var sessionFactory = context.getSessionFactory();

		final List<ResultSetMapping> resultMappings = CollectionHelper.arrayList( resultSetMappingClasses.length );
		for ( int i = 0; i < resultSetMappingClasses.length; i++ ) {
			final var clazz = resultSetMappingClasses[i];
			final ResultSetMapping clazzMapping =
					makeResultSetMapping( procedureName, clazz, querySpaceConsumer, context );
			resultMappings.add( clazzMapping );
		}
		return resultMappings;
	}

	private static void applyResultClass(
			Class<?> clazz,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		// JPA says this class can be one of 3 things -
		//		1. An entity type
		//		2. A basic type
		//		3. A constructable type

		final var mappingMetamodel = context.getMappingMetamodel();
		final var javaTypeRegistry = mappingMetamodel.getTypeConfiguration().getJavaTypeRegistry();
		final var entityDescriptor = mappingMetamodel.findEntityDescriptor( clazz );
		if ( entityDescriptor != null ) {
			resultSetMapping.addResultBuilder( new EntityDomainResultBuilder( entityDescriptor ) );
			if ( querySpaceConsumer != null ) {
				for ( String querySpace : entityDescriptor.getSynchronizedQuerySpaces() ) {
					querySpaceConsumer.accept( querySpace );
				}
			}
		}
		else {
			final var clazzJavaType = javaTypeRegistry.resolveDescriptor( clazz );
			if ( clazzJavaType != null ) {
				// NOTE: at this point we do not know whether to handle `clazzJavaType`
				// as a basic or constructor.  here, that would require a special
				// delayed result-builder.  for now, just assume a basic mapping.
				// todo (jpa4) : revisit this
				resultSetMapping.addResultBuilder( new ScalarDomainResultBuilder<>( clazzJavaType ) );
			}
		}
	}

	public static void resolveResultSetMappingClasses(
			Class<?>[] resultSetMappingClasses,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final var mappingMetamodel = context.getMappingMetamodel();
		final var javaTypeRegistry = mappingMetamodel.getTypeConfiguration().getJavaTypeRegistry();
		for ( var resultSetMappingClass : resultSetMappingClasses ) {
			final var entityDescriptor = mappingMetamodel.findEntityDescriptor( resultSetMappingClass );
			if ( entityDescriptor != null ) {
				resultSetMapping.addResultBuilder( new EntityDomainResultBuilder( entityDescriptor ) );
				for ( String querySpace : entityDescriptor.getSynchronizedQuerySpaces() ) {
					querySpaceConsumer.accept( querySpace );
				}
			}
			else {
				final var basicType = javaTypeRegistry.resolveDescriptor( resultSetMappingClass );
				if ( basicType != null ) {
					resultSetMapping.addResultBuilder( new ScalarDomainResultBuilder<>( basicType ) );
				}
			}
		}
	}
}
