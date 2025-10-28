/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.function.Consumer;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.UnknownSqlResultSetMappingException;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

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
			Class[] resultSetMappingClasses,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		if ( ! ArrayHelper.isEmpty( resultSetMappingNames ) ) {
			// cannot specify both
			if ( ! ArrayHelper.isEmpty( resultSetMappingClasses ) ) {
				throw new IllegalArgumentException( "Cannot specify both result-set mapping names and classes" );
			}
			resolveResultSetMappingNames( resultSetMappingNames, resultSetMapping, querySpaceConsumer, context );
		}
		else if ( ! ArrayHelper.isEmpty( resultSetMappingClasses ) ) {
			resolveResultSetMappingClasses( resultSetMappingClasses, resultSetMapping, querySpaceConsumer, context );
		}

		// otherwise, nothing to resolve
	}

	public static void resolveResultSetMappingNames(
			String[] resultSetMappingNames,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final NamedObjectRepository namedObjectRepository = context.getNamedObjectRepository();
		for ( String resultSetMappingName : resultSetMappingNames ) {
			final NamedResultSetMappingMemento memento =
					namedObjectRepository.getResultSetMappingMemento( resultSetMappingName );
			if ( memento == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}
			memento.resolve(
					resultSetMapping,
					querySpaceConsumer,
					context
			);
		}
	}

	public static void resolveResultSetMappingClasses(
			Class[] resultSetMappingClasses,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final MappingMetamodel mappingMetamodel = context.getMappingMetamodel();
		final JavaTypeRegistry javaTypeRegistry = mappingMetamodel.getTypeConfiguration().getJavaTypeRegistry();

		for ( Class<?> resultSetMappingClass : resultSetMappingClasses ) {
			final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( resultSetMappingClass );
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
