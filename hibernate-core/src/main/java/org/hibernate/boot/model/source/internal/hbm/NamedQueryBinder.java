/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.JAXBElement;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.internal.ImplicitResultSetMappingDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.cfg.SecondPass;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class NamedQueryBinder {
	public static void processNamedQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedQueryType namedQueryBinding) {
		processNamedQuery( context, namedQueryBinding, "" );
	}

	public static void processNamedQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedQueryType namedQueryBinding,
			String prefix) {
		String query = null;
		java.util.Map<String,String> parameterTypeMap = null;

		for ( Object content : namedQueryBinding.getContent() ) {
			if ( String.class.isInstance( content ) ) {
				String trimmed = ((String)content).trim();
				if (!"".equals(trimmed)) {
					query = trimmed;
				}
			}
			else {
				final JaxbHbmQueryParamType paramTypeBinding = 
						(JaxbHbmQueryParamType)((JAXBElement)content).getValue();
				if ( parameterTypeMap == null ) {
					parameterTypeMap = new HashMap<String,String>();
				}
				parameterTypeMap.put( paramTypeBinding.getName(), paramTypeBinding.getType() );
			}
		}

		if ( query == null ) {
			throw new org.hibernate.boot.MappingException(
					String.format(
							"Named query [%s] did not specify query string",
							namedQueryBinding.getName()
					),
					context.getOrigin()
			);
		}
		context.getMetadataCollector().addNamedQuery(
				new NamedQueryDefinitionBuilder()
						.setName( prefix + namedQueryBinding.getName() )
						.setQuery( query )
						.setComment( namedQueryBinding.getComment() )
						.setCacheable( namedQueryBinding.isCacheable() )
						.setCacheMode( namedQueryBinding.getCacheMode() )
						.setCacheRegion( namedQueryBinding.getCacheRegion() )
						.setTimeout( namedQueryBinding.getTimeout() )
						.setReadOnly( namedQueryBinding.isReadOnly() )
						.setFlushMode( namedQueryBinding.getFlushMode() )
						.setFetchSize( namedQueryBinding.getFetchSize() )
						.setParameterTypes( parameterTypeMap )
						.createNamedQueryDefinition()
		);
	}

	public static void processNamedNativeQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding) {
		processNamedNativeQuery( context, namedQueryBinding, "" );
	}

	public static void processNamedNativeQuery(
			final HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			String prefix) {
		final String queryName = prefix + namedQueryBinding.getName();
		final NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder()
				.setName( queryName )
				.setComment( namedQueryBinding.getComment() )
				.setCacheable( namedQueryBinding.isCacheable() )
				.setCacheMode( namedQueryBinding.getCacheMode() )
				.setCacheRegion( namedQueryBinding.getCacheRegion() )
				.setTimeout( namedQueryBinding.getTimeout() )
				.setReadOnly( namedQueryBinding.isReadOnly() )
				.setFlushMode( namedQueryBinding.getFlushMode() )
				.setFetchSize( namedQueryBinding.getFetchSize() )
				.setCallable( namedQueryBinding.isCallable() )
				.setResultSetRef( namedQueryBinding.getResultsetRef() );

		final ImplicitResultSetMappingDefinition.Builder implicitResultSetMappingBuilder
				= new ImplicitResultSetMappingDefinition.Builder( queryName );

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

			// Building a ResultSet mapping needs access to entity bindings for any entity
			// returns it defines.  But binding for those entities may have not been
			// completed yet.  For "normal" ResultSet mappings, this is already handled by
			// the fact that MetadataSourceProcessor#processResultSetMappings() is called
			// afterQuery all entity hierarchies have been processed.  However, here we are in
			// the middle of processing named-queries (either top-level or entity-level)
			// and have no guarantee that any entity bindings we may need here are bound.
			// So we add the second-pass to bind the implicit resultSet mapping.
			//
			// It is possible to know here whether the second-pass is needed or whether we
			// can immediately bind the ResultSet mapping.
			// todo : consider implementing this (^^) checking

			final ImplicitResultSetMappingDefinition implicitResultSetMappingDefinition = implicitResultSetMappingBuilder.build();
			builder.setResultSetRef( implicitResultSetMappingDefinition.getName() );
			context.getMetadataCollector().addSecondPass(
					new SecondPass() {
						@Override
						public void doSecondPass(Map persistentClasses) throws MappingException {
							final ResultSetMappingDefinition resultSetMappingDefinition =
									ResultSetMappingBinder.bind( implicitResultSetMappingDefinition, context );
							context.getMetadataCollector().addResultSetMapping( resultSetMappingDefinition );
							NativeSQLQueryReturn[] newQueryReturns = resultSetMappingDefinition.getQueryReturns();
							final NamedSQLQueryDefinition queryDefinition =
									context.getMetadataCollector().getNamedNativeQueryDefinition( queryName );
							if ( queryDefinition != null ) {
								queryDefinition.addQueryReturns( newQueryReturns );
							}
						}
					}
			);
		}

		context.getMetadataCollector().addNamedNativeQuery( builder.createNamedQueryDefinition() );
	}

	private static boolean processNamedQueryContentItem(
			Object content,
			NamedSQLQueryDefinitionBuilder builder,
			ImplicitResultSetMappingDefinition.Builder implicitResultSetMappingBuilder,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			HbmLocalMetadataBuildingContext context) {
		if ( String.class.isInstance( content ) ) {
			// Especially when the query string is wrapped in CDATA we will get
			// "extra" Strings here containing just spaces and/or newlines.  This
			// bit tries to account for them.
			final String contentString = StringHelper.nullIfEmpty( ( (String) content ).trim() );
			if ( contentString != null ) {
				builder.setQuery( (String) content );
				return true;
			}
			else {
				return false;
			}
		}
		else if ( JAXBElement.class.isInstance( content ) ) {
			return processNamedQueryContentItem(
					( (JAXBElement) content ).getValue(),
					builder,
					implicitResultSetMappingBuilder,
					namedQueryBinding,
					context
			);
		}

		if ( JaxbHbmQueryParamType.class.isInstance( content ) ) {
			final JaxbHbmQueryParamType paramTypeBinding = (JaxbHbmQueryParamType) content;
			builder.addParameterType( paramTypeBinding.getName(), paramTypeBinding.getType() );
		}
		else if ( JaxbHbmSynchronizeType.class.isInstance( content ) ) {
			final JaxbHbmSynchronizeType synchronizedSpace = (JaxbHbmSynchronizeType) content;
			builder.addSynchronizedQuerySpace( synchronizedSpace.getTable() );
		}
		else if ( JaxbHbmNativeQueryScalarReturnType.class.isInstance( content ) ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryScalarReturnType) content );
		}
		else if ( JaxbHbmNativeQueryReturnType.class.isInstance( content ) ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryReturnType) content );
		}
		else if ( JaxbHbmNativeQueryJoinReturnType.class.isInstance( content ) ) {
			implicitResultSetMappingBuilder.addReturn( (JaxbHbmNativeQueryJoinReturnType) content );
		}
		else if ( JaxbHbmNativeQueryCollectionLoadReturnType.class.isInstance( content ) ) {
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
