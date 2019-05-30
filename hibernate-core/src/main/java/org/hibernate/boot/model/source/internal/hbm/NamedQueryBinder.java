/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Locale;
import javax.xml.bind.JAXBElement;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.spi.HbmResultSetMappingDefinition;
import org.hibernate.boot.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.spi.NamedNativeQueryDefinition;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
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

		final NamedHqlQueryDefinition.Builder queryBuilder = new NamedHqlQueryDefinition.Builder( registrationName )
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
			if ( content instanceof String ) {
				final String hqlString = StringHelper.nullIfEmpty( ( (String) content ).trim() );
				if ( !StringHelper.isEmpty( hqlString ) ) {
					queryBuilder.setHqlString( hqlString );
					foundQuery = true;
				}
			}
			else {
				final JaxbHbmQueryParamType paramTypeBinding = (JaxbHbmQueryParamType)( (JAXBElement) content ).getValue();
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

	public static void processNamedNativeQuery(HbmLocalMetadataBuildingContext context, JaxbHbmNamedNativeQueryType namedQueryBinding) {
		processNamedNativeQuery( context, namedQueryBinding, "" );
	}

	public static void processNamedNativeQuery(
			final HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			String prefix) {
		if ( namedQueryBinding.isCallable() ) {
			DeprecationLogger.DEPRECATION_LOGGER.warn(
					"Marking named native queries as callable is no longer supported; use `@javax.persistence.NamedStoredProcedureQuery` instead.  Ignoring."
			);
		}

		final String registrationName = prefix + namedQueryBinding.getName();

		final NamedNativeQueryDefinition.Builder builder = new NamedNativeQueryDefinition.Builder( registrationName )
				.setComment( namedQueryBinding.getComment() )
				.setCacheable( namedQueryBinding.isCacheable() )
				.setCacheMode( namedQueryBinding.getCacheMode() )
				.setCacheRegion( namedQueryBinding.getCacheRegion() )
				.setTimeout( namedQueryBinding.getTimeout() )
				.setReadOnly( namedQueryBinding.isReadOnly() )
				.setFlushMode( namedQueryBinding.getFlushMode() )
				.setFetchSize( namedQueryBinding.getFetchSize() )
				.setResultSetMappingName( namedQueryBinding.getResultsetRef() );

		final HbmResultSetMappingDefinition.Builder implicitResultSetMappingBuilder = new HbmResultSetMappingDefinition.Builder( registrationName );

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
			if ( StringHelper.isNotEmpty( namedQueryBinding.getResultsetRef() ) ) {
				throw new org.hibernate.boot.MappingException(
						String.format(
								"Named native query [%s] specified both a resultset-ref and an inline mapping of results",
								namedQueryBinding.getName()
						),
						context.getOrigin()
				);
			}

			context.getMetadataCollector().addResultSetMapping( implicitResultSetMappingBuilder.build( context ) );
		}

		context.getMetadataCollector().addNamedNativeQuery( builder.build() );
	}

	private static boolean processNamedQueryContentItem(
			Object content,
			NamedNativeQueryDefinition.Builder queryBuilder,
			HbmResultSetMappingDefinition.Builder implicitResultSetMappingBuilder,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			HbmLocalMetadataBuildingContext context) {
		if ( content instanceof String ) {
			// Especially when the query string is wrapped in CDATA we will get
			// "extra" Strings here containing just spaces and/or newlines.  This
			// bit tries to account for them.
			final String contentString = StringHelper.nullIfEmpty( ( (String) content ).trim() );
			if ( contentString != null ) {
				queryBuilder.setSqlString( (String) content );
				return true;
			}
			else {
				return false;
			}
		}
		else if ( content instanceof JAXBElement ) {
			return processNamedQueryContentItem(
					( (JAXBElement) content ).getValue(),
					queryBuilder,
					implicitResultSetMappingBuilder,
					namedQueryBinding,
					context
			);
		}

		if ( content instanceof JaxbHbmQueryParamType ) {
			final JaxbHbmQueryParamType paramTypeBinding = (JaxbHbmQueryParamType) content;
			queryBuilder.addParameterTypeHint( paramTypeBinding.getName(), paramTypeBinding.getType() );
		}
		else if ( content instanceof JaxbHbmSynchronizeType ) {
			final JaxbHbmSynchronizeType synchronizedSpace = (JaxbHbmSynchronizeType) content;
			queryBuilder.addSynchronizedQuerySpace( synchronizedSpace.getTable() );
		}
		else if ( content instanceof JaxbHbmNativeQueryScalarReturnType ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryScalarReturnType) content );
		}
		else if ( content instanceof JaxbHbmNativeQueryReturnType ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryReturnType) content );
		}
		else if ( content instanceof JaxbHbmNativeQueryJoinReturnType ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryJoinReturnType) content );
		}
		else if ( content instanceof JaxbHbmNativeQueryCollectionLoadReturnType ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryCollectionLoadReturnType) content );
		}
		else {
			throw new org.hibernate.boot.MappingException(
					String.format(
							Locale.ENGLISH,
							"Encountered unexpected content type [%s] for named native query [%s] : [%s]",
							content.getClass().getName(),
							namedQueryBinding.getName(),
							content.toString()
					),
					context.getOrigin()
			);
		}

		return false;
	}
}
