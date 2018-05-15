/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.bind.JAXBElement;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.model.query.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.model.query.internal.NamedNativeQueryDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.EntityResultDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.FetchDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.ResultSetMappingDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.ScalarResultDefinitionImpl;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class NamedQueryBinder {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( NamedQueryBinder.class );

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

		final String queryName = prefix + namedQueryBinding.getName();

		context.getMetadataCollector().addNamedHqlQuery(
				new NamedHqlQueryDefinitionImpl.Builder( queryName, query )
						.setComment( namedQueryBinding.getComment() )
						.setCacheable( namedQueryBinding.isCacheable() )
						.setCacheMode( namedQueryBinding.getCacheMode() )
						.setCacheRegion( namedQueryBinding.getCacheRegion() )
						.setTimeout( namedQueryBinding.getTimeout() )
						.setReadOnly( namedQueryBinding.isReadOnly() )
						.setFlushMode( namedQueryBinding.getFlushMode() )
						.setFetchSize( namedQueryBinding.getFetchSize() )
						.build()
		);

	}

	public static void processNamedNativeQuery(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding) {
		processNamedNativeQuery( context, namedQueryBinding, "" );
	}

	private enum ContentCategory {
		QUERY,
		SYNCHRONIZED_TABLE_NAME,
		PARAM,
		RESULT,
		OTHER
	}

	private static long inLineResultMappingCount = 0;

	public static void processNamedNativeQuery(
			final HbmLocalMetadataBuildingContext context,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			String prefix) {
		final String queryName = prefix + namedQueryBinding.getName();

		if ( namedQueryBinding.isCallable() ) {
			log.warnNativeQueryAsCallable();
		}

		final NamedNativeQueryDefinitionImpl.Builder builder = new NamedNativeQueryDefinitionImpl.Builder( queryName )
				.setComment( namedQueryBinding.getComment() )
				.setCacheable( namedQueryBinding.isCacheable() )
				.setCacheMode( namedQueryBinding.getCacheMode() )
				.setCacheRegion( namedQueryBinding.getCacheRegion() )
				.setTimeout( namedQueryBinding.getTimeout() )
				.setReadOnly( namedQueryBinding.isReadOnly() )
				.setFlushMode( namedQueryBinding.getFlushMode() )
				.setFetchSize( namedQueryBinding.getFetchSize() );

		final ResultSetMappingDefinitionImpl inLineResultMapping = new ResultSetMappingDefinitionImpl(
				"in-line-mapping:" + ++inLineResultMappingCount
		);

		boolean foundQuery = false;
		boolean foundInLineResult = false;

		for ( Serializable contentItem : namedQueryBinding.getContent() ) {
			final ContentCategory contentCategory = processNamedQueryContentItem(
					contentItem,
					builder,
					inLineResultMapping,
					namedQueryBinding,
					context
			);

			foundQuery = foundQuery || contentCategory == ContentCategory.QUERY;
			foundInLineResult = foundInLineResult || contentCategory == ContentCategory.RESULT;
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

		if ( foundInLineResult ) {
			if ( StringHelper.isNotEmpty( namedQueryBinding.getResultsetRef() ) ) {
				throw new org.hibernate.boot.MappingException(
						String.format(
								"Named native query [%s] specified both a result set mapping and an inline mapping of results",
								namedQueryBinding.getName()
						),
						context.getOrigin()
				);
			}

			context.getMetadataCollector().addResultSetMapping( inLineResultMapping );
			builder.setResultSetMapping( inLineResultMapping.getName() );
		}
		else {
//			// Building a ResultSet mapping needs access to entity bindings for any entity
//			// returns it defines.  But binding for those entities may have not been
//			// completed yet.  For "normal" ResultSet mappings, this is already handled by
//			// the fact that MetadataSourceProcessor#processResultSetMappings() is called
//			// after all entity hierarchies have been processed.  However, here we are in
//			// the middle of processing named-queries (either top-level or entity-level)
//			// and have no guarantee that any entity bindings we may need here are bound.
//			// So we add the second-pass to bind the implicit resultSet mapping.
//			//
//			// It is possible to know here whether the second-pass is needed or whether we
//			// can immediately bind the ResultSet mapping.
//			// todo : consider implementing this (^^) checking
//
//			final ImplicitResultSetMappingDefinition implicitResultSetMappingDefinition = implicitResultSetMappingBuilder.build();
//			builder.setResultSetRef( implicitResultSetMappingDefinition.getName() );
//			context.getMetadataCollector().addSecondPass(
//					new SecondPass() {
//						@Override
//						public void doSecondPass(Map persistentClasses) throws MappingException {
//							final ResultSetMappingDefinition resultSetMappingDefinition =
//									ResultSetMappingBinder.bind( implicitResultSetMappingDefinition, context );
//							context.getMetadataCollector().addResultSetMapping( resultSetMappingDefinition );
//							NativeSQLQueryReturn[] newQueryReturns = resultSetMappingDefinition.getQueryReturns();
//							final NamedSQLQueryDefinition queryDefinition =
//									context.getMetadataCollector().getNamedNativeQueryDefinition( queryName );
//							if ( queryDefinition != null ) {
//								queryDefinition.addQueryReturns( newQueryReturns );
//							}
//						}
//					}
//			);

			builder.setResultSetMapping( namedQueryBinding.getResultsetRef() );
		}

		context.getMetadataCollector().addNamedNativeQuery( builder.build() );
	}

	private static ContentCategory processNamedQueryContentItem(
			Object content,
			NamedNativeQueryDefinitionImpl.Builder builder,
			ResultSetMappingDefinitionImpl inLineResultMapping,
			JaxbHbmNamedNativeQueryType namedQueryBinding,
			HbmLocalMetadataBuildingContext context) {
		if ( content == null ) {
			return ContentCategory.OTHER;
		}

		if ( String.class.isInstance( content ) ) {
			// Especially when the query string is wrapped in CDATA we will get
			// "extra" Strings here containing just spaces and/or newlines.  This
			// bit tries to account for them.
			final String contentString = StringHelper.nullIfEmpty( ( (String) content ).trim() );
			if ( contentString != null ) {
				builder.setSqlString( (String) content );
				return ContentCategory.QUERY;
			}
			else {
				return ContentCategory.OTHER;
			}
		}

		if ( JAXBElement.class.isInstance( content ) ) {
			return processNamedQueryContentItem(
					( (JAXBElement) content ).getValue(),
					builder,
					inLineResultMapping,
					namedQueryBinding,
					context
			);
		}

		if ( JaxbHbmQueryParamType.class.isInstance( content ) ) {
			// todo (6.0) : need to figure this out
			// 		most likely a simple boot model definition for query parameters in named query
			//final JaxbHbmQueryParamType paramTypeBinding = (JaxbHbmQueryParamType) content;
			//builder.addParameterType( paramTypeBinding.getName(), paramTypeBinding.getType() );
			throw new NotYetImplementedFor6Exception();
		}

		if ( JaxbHbmSynchronizeType.class.isInstance( content ) ) {
			final JaxbHbmSynchronizeType synchronizedSpace = (JaxbHbmSynchronizeType) content;
			builder.addQuerySpace( synchronizedSpace.getTable() );
			return ContentCategory.SYNCHRONIZED_TABLE_NAME;
		}

		if ( JaxbHbmNativeQueryScalarReturnType.class.isInstance( content ) ) {
			final JaxbHbmNativeQueryScalarReturnType scalarResultElement
					= (JaxbHbmNativeQueryScalarReturnType) content;

			inLineResultMapping.addResult(
					new ScalarResultDefinitionImpl( scalarResultElement.getColumn(), scalarResultElement.getType() )
			);

			return ContentCategory.RESULT;
		}

		if ( JaxbHbmNativeQueryReturnType.class.isInstance( content ) ) {
			final JaxbHbmNativeQueryReturnType entityResultElement
					= (JaxbHbmNativeQueryReturnType) content;

			inLineResultMapping.addResult(
					new EntityResultDefinitionImpl(
							entityResultElement.getEntityName(),
							entityResultElement.getClazz(),
							entityResultElement.getAlias()
					)
			);

			return ContentCategory.RESULT;
		}

		if ( JaxbHbmNativeQueryJoinReturnType.class.isInstance( content ) ) {
			final JaxbHbmNativeQueryJoinReturnType joinResultElement
					= (JaxbHbmNativeQueryJoinReturnType) content;

			inLineResultMapping.addFetch(
					new FetchDefinitionImpl(
							joinResultElement.getAlias(),
							null,
							joinResultElement.getProperty(),
							joinResultElement.getLockMode()
					)
			);

			return ContentCategory.RESULT;
		}

		if ( JaxbHbmNativeQueryCollectionLoadReturnType.class.isInstance( content ) ) {
			final JaxbHbmNativeQueryCollectionLoadReturnType collectionLoadReturnElement
					= (JaxbHbmNativeQueryCollectionLoadReturnType) content;

			inLineResultMapping.addResult(
					ResultSetMappingBinder.extractReturnDescription(
							collectionLoadReturnElement,
							context
					)
			);

			return ContentCategory.RESULT;
		}


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
}
