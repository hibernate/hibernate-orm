/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.QuerySplitter;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.ParameterTranslations;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.EmptyIterator;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.type.Type;

/**
 * Defines a query execution plan for an HQL query (or filter).
 *
 * @author Steve Ebersole
 */
public class HQLQueryPlan implements Serializable {

    // TODO : keep separate notions of QT[] here for shallow/non-shallow queries...

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, HQLQueryPlan.class.getName());

	private final String sourceQuery;
	private final QueryTranslator[] translators;
	private final String[] sqlStrings;

	private final ParameterMetadata parameterMetadata;
	private final ReturnMetadata returnMetadata;
	private final Set querySpaces;

	private final Set enabledFilterNames;
	private final boolean shallow;

	public HQLQueryPlan(String hql, boolean shallow, Map enabledFilters, SessionFactoryImplementor factory) {
		this( hql, null, shallow, enabledFilters, factory );
	}

	protected HQLQueryPlan(String hql, String collectionRole, boolean shallow, Map enabledFilters,  SessionFactoryImplementor factory){
		this.sourceQuery = hql;
		this.shallow = shallow;
		Set copy = new HashSet();
		copy.addAll( enabledFilters.keySet() );
		this.enabledFilterNames = java.util.Collections.unmodifiableSet( copy );

		final String[] concreteQueryStrings = QuerySplitter.concreteQueries( hql, factory );
		final int length = concreteQueryStrings.length;
		this.translators = new QueryTranslator[length];

		List<String> sqlStringList = new ArrayList<String>();
		Set combinedQuerySpaces = new HashSet();

		final boolean hasCollectionRole = (collectionRole == null);
		final Map querySubstitutions = factory.getSettings().getQuerySubstitutions();
		final QueryTranslatorFactory queryTranslatorFactory = factory.getSettings().getQueryTranslatorFactory();

		for ( int i=0; i<length; i++ ) {
			if ( hasCollectionRole ) {
				translators[i] = queryTranslatorFactory
						.createQueryTranslator( hql, concreteQueryStrings[i], enabledFilters, factory );
				translators[i].compile( querySubstitutions, shallow );
			}
			else {
				translators[i] = queryTranslatorFactory
						.createFilterTranslator( hql, concreteQueryStrings[i], enabledFilters, factory );
				( ( FilterTranslator ) translators[i] ).compile( collectionRole, querySubstitutions, shallow );
			}
			combinedQuerySpaces.addAll( translators[i].getQuerySpaces() );
			sqlStringList.addAll( translators[i].collectSqlStrings() );
		}

		this.sqlStrings = ArrayHelper.toStringArray( sqlStringList );
		this.querySpaces = combinedQuerySpaces;

		if ( length == 0 ) {
			parameterMetadata = new ParameterMetadata( null, null );
			returnMetadata = null;
		}
		else {
			this.parameterMetadata = buildParameterMetadata( translators[0].getParameterTranslations(), hql );
			if ( translators[0].isManipulationStatement() ) {
				returnMetadata = null;
			}
			else {
				final Type[] types = ( length > 1 ) ? new Type[translators[0].getReturnTypes().length] : translators[0].getReturnTypes();
				returnMetadata = new ReturnMetadata( translators[0].getReturnAliases(), types );
			}
		}
	}

	public String getSourceQuery() {
		return sourceQuery;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	public ReturnMetadata getReturnMetadata() {
		return returnMetadata;
	}

	public Set getEnabledFilterNames() {
		return enabledFilterNames;
	}

	public String[] getSqlStrings() {
		return sqlStrings;
	}

	public Set getUtilizedFilterNames() {
		// TODO : add this info to the translator and aggregate it here...
		return null;
	}

	public boolean isShallow() {
		return shallow;
	}

	public List performList(
			QueryParameters queryParameters,
	        SessionImplementor session) throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Find: {0}", getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		boolean hasLimit = queryParameters.getRowSelection() != null &&
		                   queryParameters.getRowSelection().definesLimits();
		boolean needsLimit = hasLimit && translators.length > 1;
		QueryParameters queryParametersToUse;
		if ( needsLimit ) {
			LOG.needsLimit();
			RowSelection selection = new RowSelection();
			selection.setFetchSize( queryParameters.getRowSelection().getFetchSize() );
			selection.setTimeout( queryParameters.getRowSelection().getTimeout() );
			queryParametersToUse = queryParameters.createCopyUsing( selection );
		}
		else {
			queryParametersToUse = queryParameters;
		}

		List combinedResults = new ArrayList();
		IdentitySet distinction = new IdentitySet();
		int includedCount = -1;
		translator_loop:
		for ( QueryTranslator translator : translators ) {
			List tmp = translator.list( session, queryParametersToUse );
			if ( needsLimit ) {
				// NOTE : firstRow is zero-based
				final int first = queryParameters.getRowSelection().getFirstRow() == null
						? 0
						: queryParameters.getRowSelection().getFirstRow();
				final int max = queryParameters.getRowSelection().getMaxRows() == null
						? -1
						: queryParameters.getRowSelection().getMaxRows();
				for ( final Object result : tmp ) {
					if ( !distinction.add( result ) ) {
						continue;
					}
					includedCount++;
					if ( includedCount < first ) {
						continue;
					}
					combinedResults.add( result );
					if ( max >= 0 && includedCount > max ) {
						// break the outer loop !!!
						break translator_loop;
					}
				}
			}
			else {
				combinedResults.addAll( tmp );
			}
		}
		return combinedResults;
	}

	public Iterator performIterate(
			QueryParameters queryParameters,
	        EventSource session) throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Iterate: {0}", getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		if ( translators.length == 0 ) {
			return EmptyIterator.INSTANCE;
		}

		Iterator[] results = null;
		boolean many = translators.length > 1;
		if ( many ) {
			results = new Iterator[translators.length];
		}

		Iterator result = null;
		for ( int i = 0; i < translators.length; i++ ) {
			result = translators[i].iterate( queryParameters, session );
			if ( many ) {
				results[i] = result;
			}
		}

		return many ? new JoinedIterator(results) : result;
	}

	public ScrollableResults performScroll(
			QueryParameters queryParameters,
	        SessionImplementor session) throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Iterate: {0}", getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		if ( translators.length != 1 ) {
			throw new QueryException( "implicit polymorphism not supported for scroll() queries" );
		}
		if ( queryParameters.getRowSelection().definesLimits() && translators[0].containsCollectionFetches() ) {
			throw new QueryException( "firstResult/maxResults not supported in conjunction with scroll() of a query containing collection fetches" );
		}

		return translators[0].scroll( queryParameters, session );
	}

	public int performExecuteUpdate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Execute update: {0}", getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		if ( translators.length != 1 ) {
			LOG.splitQueries( getSourceQuery(), translators.length );
		}
		int result = 0;
		for ( QueryTranslator translator : translators ) {
			result += translator.executeUpdate( queryParameters, session );
		}
		return result;
	}

	private ParameterMetadata buildParameterMetadata(ParameterTranslations parameterTranslations, String hql) {
		long start = System.currentTimeMillis();
		ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( hql );
		long end = System.currentTimeMillis();
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "HQL param location recognition took {0} mills ({1})", ( end - start ), hql );
		}

		int ordinalParamCount = parameterTranslations.getOrdinalParameterCount();
		int[] locations = ArrayHelper.toIntArray( recognizer.getOrdinalParameterLocationList() );
		if ( parameterTranslations.supportsOrdinalParameterMetadata() && locations.length != ordinalParamCount ) {
			throw new HibernateException( "ordinal parameter mismatch" );
		}
		ordinalParamCount = locations.length;
		OrdinalParameterDescriptor[] ordinalParamDescriptors = new OrdinalParameterDescriptor[ordinalParamCount];
		for ( int i = 1; i <= ordinalParamCount; i++ ) {
			ordinalParamDescriptors[ i - 1 ] = new OrdinalParameterDescriptor(
					i,
			        parameterTranslations.supportsOrdinalParameterMetadata()
		                    ? parameterTranslations.getOrdinalParameterExpectedType( i )
		                    : null,
			        locations[ i - 1 ]
			);
		}

		Iterator itr = recognizer.getNamedParameterDescriptionMap().entrySet().iterator();
		Map<String, NamedParameterDescriptor> namedParamDescriptorMap = new HashMap<String, NamedParameterDescriptor>();
		while( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final String name = ( String ) entry.getKey();
			final ParamLocationRecognizer.NamedParameterDescription description =
					( ParamLocationRecognizer.NamedParameterDescription ) entry.getValue();
			namedParamDescriptorMap.put(
					name,
					new NamedParameterDescriptor(
							name,
					        parameterTranslations.getNamedParameterExpectedType( name ),
					        description.buildPositionsArray(),
					        description.isJpaStyle()
					)
			);
		}

		return new ParameterMetadata( ordinalParamDescriptors, namedParamDescriptorMap );
	}
	public QueryTranslator[] getTranslators() {
		QueryTranslator[] copy = new QueryTranslator[translators.length];
		System.arraycopy(translators, 0, copy, 0, copy.length);
		return copy;
	}

	public Class getDynamicInstantiationResultType() {
		return translators[0].getDynamicInstantiationResultType();
	}
}
