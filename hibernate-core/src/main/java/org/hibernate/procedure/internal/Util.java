/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.function.Consumer;

import org.hibernate.query.UnknownSqlResultSetMappingException;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultSetMapping;

import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;

/**
 * Utilities used to implement procedure call support.
 *
 * @author Steve Ebersole
 */
public class Util {

	private Util() {
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
