/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.query.ImplicitHbmResultSetMappingDescriptorBuilder;

import jakarta.xml.bind.JAXBElement;
import org.hibernate.query.QueryFlushMode;

import static org.hibernate.boot.model.internal.QueryBinder.createStoredProcedure;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfBlank;

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
		final var queryBuilder =
				new HqlQueryBuilder<>( registrationName )
						.setComment( namedQueryBinding.getComment() )
						.setCacheable( namedQueryBinding.isCacheable() )
						.setCacheMode( namedQueryBinding.getCacheMode() )
						.setCacheRegion( namedQueryBinding.getCacheRegion() )
						.setTimeout( namedQueryBinding.getTimeout() )
						.setReadOnly( namedQueryBinding.isReadOnly() )
						.setFlushMode( QueryFlushMode.fromHibernateMode( namedQueryBinding.getFlushMode() ) )
						.setFetchSize( namedQueryBinding.getFetchSize() );

		boolean foundQuery = false;

		for ( Object content : namedQueryBinding.getContent() ) {
			if ( content instanceof String string ) {
				if ( isNotBlank( string ) ) {
					queryBuilder.setHqlString( string.trim() );
					foundQuery = true;
				}
			}
			else if ( content instanceof JAXBElement<?> element
					&& element.getValue() instanceof JaxbHbmQueryParamType queryParamType ) {
				queryBuilder.addParameterTypeHint( queryParamType.getName(), queryParamType.getType() );
			}
			else {
				throw new AssertionFailure( "Unexpected content type: " + content.getClass().getName() );
			}
		}

		if ( !foundQuery ) {
			throw new MappingException(
					"Named query [%s] did not specify query string"
							.formatted( namedQueryBinding.getName() ),
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
			DEPRECATION_LOGGER.callableNamedNativeQuery();
		}

		final String registrationName = prefix + namedQueryBinding.getName();
		final var builder =
				new NativeQueryBuilder<>( registrationName )
						.setComment( namedQueryBinding.getComment() )
						.setCacheable( namedQueryBinding.isCacheable() )
						.setCacheMode( namedQueryBinding.getCacheMode() )
						.setCacheRegion( namedQueryBinding.getCacheRegion() )
						.setTimeout( namedQueryBinding.getTimeout() )
						.setReadOnly( namedQueryBinding.isReadOnly() )
						.setFlushMode( QueryFlushMode.fromHibernateMode( namedQueryBinding.getFlushMode() ) )
						.setFetchSize( namedQueryBinding.getFetchSize() )
						.setResultSetMappingName( namedQueryBinding.getResultsetRef() );

		final var implicitResultSetMappingBuilder =
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
			throw new MappingException(
					"Named native query [%s] did not specify query string"
							.formatted( namedQueryBinding.getName() ),
					context.getOrigin()
			);
		}

		final var collector = context.getMetadataCollector();

		if ( implicitResultSetMappingBuilder.hasAnyReturns() ) {
			if ( isNotEmpty( namedQueryBinding.getResultsetRef() ) ) {
				throw new MappingException(
						"Named native query [%s] specified both a resultset-ref and an inline mapping of results"
								.formatted( namedQueryBinding.getName() ),
						context.getOrigin()
				);
			}

			collector.addResultSetMapping( implicitResultSetMappingBuilder.build( context ) );
			builder.setResultSetMappingName( implicitResultSetMappingBuilder.getRegistrationName() );
		}

		if ( namedQueryBinding.isCallable() ) {
			final var definition =
					createStoredProcedure( builder, context,
							() -> illegalCallSyntax( context, namedQueryBinding, builder.getSqlString() ) );
			collector.addNamedProcedureCallDefinition( definition );
			DEPRECATION_LOGGER.callableNamedNativeQuery();
		}
		else {
			collector.addNamedNativeQuery( builder.build() );
		}
	}

	private static MappingException illegalCallSyntax(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			String sqlString) {
		return new MappingException(
				"Callable named native query [%s] doesn't use the JDBC call syntax: %s"
						.formatted( namedQueryBinding.getName(), sqlString ),
				context.getOrigin()
		);
	}

	private static boolean processNamedQueryContentItem(
			Object content,
			NativeQueryBuilder<?> queryBuilder,
			ImplicitHbmResultSetMappingDescriptorBuilder implicitResultSetMappingBuilder,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			HbmLocalMetadataBuildingContext context) {
		if ( content instanceof String string ) {
			// Especially when the query string is wrapped in CDATA we will get
			// "extra" Strings here containing just spaces and/or newlines. This
			// bit tries to account for them.
			final String contentString = nullIfBlank( string );
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
			throw new MappingException(
					"Encountered unexpected content type [%s] for named native query [%s] : [%s]"
							.formatted( content.getClass().getName(), namedQueryBinding.getName(), content ),
					context.getOrigin()
			);
		}

		return false;
	}
}
