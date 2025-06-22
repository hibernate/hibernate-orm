/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Locale;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.model.internal.QueryBinder;
import org.hibernate.boot.query.ImplicitHbmResultSetMappingDescriptorBuilder;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.internal.log.DeprecationLogger;

import jakarta.xml.bind.JAXBElement;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * @author Steve Ebersole
 */
public class NamedQueryBinder {
	public static void processNamedQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedQueryType namedQueryBinding) {
		processNamedQuery( context, namedQueryBinding, "" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named HQL query

	public static void processNamedQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedQueryType namedQueryBinding,
			String prefix) {
		final String registrationName = prefix + namedQueryBinding.getName();

		final NamedHqlQueryDefinition.Builder<?> queryBuilder =
				new NamedHqlQueryDefinition.Builder<>( registrationName )
						.setComment( namedQueryBinding.getComment() )
						.setCacheable( namedQueryBinding.isCacheable() )
						.setCacheMode( namedQueryBinding.getCacheMode() )
						.setCacheRegion( namedQueryBinding.getCacheRegion() )
						.setTimeout( namedQueryBinding.getTimeout() )
						.setReadOnly( namedQueryBinding.isReadOnly() )
						.setFlushMode( namedQueryBinding.getFlushMode() )
						.setFetchSize( namedQueryBinding.getFetchSize() );

		boolean foundQuery = false;

		for ( Object content : namedQueryBinding.getContent() ) {
			if ( content instanceof String string ) {
				final String hqlString = nullIfEmpty( string.trim() );
				if ( isNotEmpty( hqlString ) ) {
					queryBuilder.setHqlString( hqlString );
					foundQuery = true;
				}
			}
			else {
				final JaxbHbmQueryParamType paramTypeBinding = (JaxbHbmQueryParamType)
						( (JAXBElement<?>) content ).getValue();
				queryBuilder.addParameterTypeHint( paramTypeBinding.getName(), paramTypeBinding.getType() );
			}
		}

		if ( ! foundQuery ) {
			throw new org.hibernate.boot.MappingException(
					String.format(
							"Named query [%s] did not specify query string",
							namedQueryBinding.getName()
					),
					context.getOrigin()
			);
		}

		context.getMetadataCollector().addNamedQuery( queryBuilder.build() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named native query

	public static void processNamedNativeQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding) {
		processNamedNativeQuery( context, namedQueryBinding, "" );
	}

	public static void processNamedNativeQuery(
			final HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			String prefix) {
		if ( namedQueryBinding.isCallable() ) {
			DeprecationLogger.DEPRECATION_LOGGER.warn(
					"Marking named native queries as callable is no longer supported; use `@jakarta.persistence.NamedStoredProcedureQuery` instead.  Ignoring."
			);
		}

		final String registrationName = prefix + namedQueryBinding.getName();

		final NamedNativeQueryDefinition.Builder<?> builder =
				new NamedNativeQueryDefinition.Builder<>( registrationName )
						.setComment( namedQueryBinding.getComment() )
						.setCacheable( namedQueryBinding.isCacheable() )
						.setCacheMode( namedQueryBinding.getCacheMode() )
						.setCacheRegion( namedQueryBinding.getCacheRegion() )
						.setTimeout( namedQueryBinding.getTimeout() )
						.setReadOnly( namedQueryBinding.isReadOnly() )
						.setFlushMode( namedQueryBinding.getFlushMode() )
						.setFetchSize( namedQueryBinding.getFetchSize() )
						.setResultSetMappingName( namedQueryBinding.getResultsetRef() );

		final ImplicitHbmResultSetMappingDescriptorBuilder implicitResultSetMappingBuilder =
				new ImplicitHbmResultSetMappingDescriptorBuilder( registrationName, context );

		boolean foundQuery = false;

		for ( Object content : namedQueryBinding.getContent() ) {
			final boolean wasQuery = processNamedQueryContentItem(
					content,
					builder,
					implicitResultSetMappingBuilder,
					namedQueryBinding,
					context
			);
			if ( wasQuery ) {
				foundQuery = true;
			}
		}

		if ( !foundQuery ) {
			throw new org.hibernate.boot.MappingException(
					String.format(
							"Named native query [%s] did not specify query string",
							namedQueryBinding.getName()
					),
					context.getOrigin()
			);
		}

		if ( implicitResultSetMappingBuilder.hasAnyReturns() ) {
			if ( isNotEmpty( namedQueryBinding.getResultsetRef() ) ) {
				throw new org.hibernate.boot.MappingException(
						String.format(
								"Named native query [%s] specified both a resultset-ref and an inline mapping of results",
								namedQueryBinding.getName()
						),
						context.getOrigin()
				);
			}

			context.getMetadataCollector().addResultSetMapping( implicitResultSetMappingBuilder.build( context ) );

			builder.setResultSetMappingName( implicitResultSetMappingBuilder.getRegistrationName() );
		}

		if ( namedQueryBinding.isCallable() ) {
			final NamedProcedureCallDefinition definition = QueryBinder.createStoredProcedure(
					builder, context,
					() -> illegalCallSyntax( context, namedQueryBinding, builder.getSqlString() )
			);
			context.getMetadataCollector().addNamedProcedureCallDefinition( definition );
			DeprecationLogger.DEPRECATION_LOGGER.warn(
					"Marking named native queries as callable is deprecated; use `<named-stored-procedure-query/>` instead."
			);
		}
		else {
			context.getMetadataCollector().addNamedNativeQuery( builder.build() );
		}
	}

	private static MappingException illegalCallSyntax(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			String sqlString) {
		return new MappingException(
				String.format(
						"Callable named native query [%s] doesn't use the JDBC call syntax: %s",
						namedQueryBinding.getName(),
						sqlString
				),
				context.getOrigin()
		);
	}

	private static boolean processNamedQueryContentItem(
			Object content,
			NamedNativeQueryDefinition.Builder<?> queryBuilder,
			ImplicitHbmResultSetMappingDescriptorBuilder implicitResultSetMappingBuilder,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			HbmLocalMetadataBuildingContext context) {
		if ( content instanceof String string ) {
			// Especially when the query string is wrapped in CDATA we will get
			// "extra" Strings here containing just spaces and/or newlines.  This
			// bit tries to account for them.
			final String contentString = nullIfEmpty( string.trim() );
			if ( contentString != null ) {
				queryBuilder.setSqlString( string );
				return true;
			}
			else {
				return false;
			}
		}
		else if ( content instanceof JAXBElement<?> element ) {
			return processNamedQueryContentItem(
					element.getValue(),
					queryBuilder,
					implicitResultSetMappingBuilder,
					namedQueryBinding,
					context
			);
		}

		if ( content instanceof JaxbHbmQueryParamType paramTypeBinding ) {
			queryBuilder.addParameterTypeHint( paramTypeBinding.getName(), paramTypeBinding.getType() );
		}
		else if ( content instanceof JaxbHbmSynchronizeType synchronizedSpace ) {
			queryBuilder.addSynchronizedQuerySpace( synchronizedSpace.getTable() );
		}
		else if ( content instanceof JaxbHbmNativeQueryScalarReturnType scalarReturnType ) {
			implicitResultSetMappingBuilder.addReturn( scalarReturnType );
		}
		else if ( content instanceof JaxbHbmNativeQueryReturnType returnType ) {
			implicitResultSetMappingBuilder.addReturn( returnType );
		}
		else if ( content instanceof JaxbHbmNativeQueryJoinReturnType jaxbHbmNativeQueryJoinReturnType ) {
			implicitResultSetMappingBuilder.addReturn( jaxbHbmNativeQueryJoinReturnType );
		}
		else if ( content instanceof JaxbHbmNativeQueryCollectionLoadReturnType collectionLoadReturnType ) {
			implicitResultSetMappingBuilder.addReturn( collectionLoadReturnType );
		}
		else {
			throw new org.hibernate.boot.MappingException(
					String.format(
							Locale.ENGLISH,
							"Encountered unexpected content type [%s] for named native query [%s] : [%s]",
							content.getClass().getName(),
							namedQueryBinding.getName(),
							content
					),
					context.getOrigin()
			);
		}

		return false;
	}
}
