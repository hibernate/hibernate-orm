package org.hibernate.engine.query;

import org.hibernate.hql.QuerySplitter;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.ParameterTranslations;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.EmptyIterator;
import org.hibernate.util.JoinedIterator;
import org.hibernate.util.IdentitySet;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.QueryException;
import org.hibernate.type.Type;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.RowSelection;
import org.hibernate.event.EventSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

/**
 * Defines a query execution plan for an HQL query (or filter).
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class HQLQueryPlan implements Serializable {

	// TODO : keep seperate notions of QT[] here for shallow/non-shallow queries...

	private static final Log log = LogFactory.getLog( HQLQueryPlan.class );

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

	protected HQLQueryPlan(String hql, String collectionRole, boolean shallow, Map enabledFilters, SessionFactoryImplementor factory) {
		this.sourceQuery = hql;
		this.shallow = shallow;

		Set copy = new HashSet();
		copy.addAll( enabledFilters.keySet() );
		this.enabledFilterNames = java.util.Collections.unmodifiableSet( copy );

		Set combinedQuerySpaces = new HashSet();
		String[] concreteQueryStrings = QuerySplitter.concreteQueries( hql, factory );
		final int length = concreteQueryStrings.length;
		translators = new QueryTranslator[length];
		List sqlStringList = new ArrayList();
		for ( int i=0; i<length; i++ ) {
			if ( collectionRole == null ) {
				translators[i] = factory.getSettings()
						.getQueryTranslatorFactory()
						.createQueryTranslator( hql, concreteQueryStrings[i], enabledFilters, factory );
				translators[i].compile( factory.getSettings().getQuerySubstitutions(), shallow );
			}
			else {
				translators[i] = factory.getSettings()
						.getQueryTranslatorFactory()
						.createFilterTranslator( hql, concreteQueryStrings[i], enabledFilters, factory );
				( ( FilterTranslator ) translators[i] ).compile( collectionRole, factory.getSettings().getQuerySubstitutions(), shallow );
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
				if ( length > 1 ) {
					final int returns = translators[0].getReturnTypes().length;
					returnMetadata = new ReturnMetadata( translators[0].getReturnAliases(), new Type[returns] );
				}
				else {
					returnMetadata = new ReturnMetadata( translators[0].getReturnAliases(), translators[0].getReturnTypes() );
				}
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
		if ( log.isTraceEnabled() ) {
			log.trace( "find: " + getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		boolean hasLimit = queryParameters.getRowSelection() != null &&
		                   queryParameters.getRowSelection().definesLimits();
		boolean needsLimit = hasLimit && translators.length > 1;
		QueryParameters queryParametersToUse;
		if ( needsLimit ) {
			log.warn( "firstResult/maxResults specified on polymorphic query; applying in memory!" );
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
		translator_loop: for ( int i = 0; i < translators.length; i++ ) {
			List tmp = translators[i].list( session, queryParametersToUse );
			if ( needsLimit ) {
				// NOTE : firstRow is zero-based
				int first = queryParameters.getRowSelection().getFirstRow() == null
				            ? 0
			                : queryParameters.getRowSelection().getFirstRow().intValue();
				int max = queryParameters.getRowSelection().getMaxRows() == null
				            ? -1
			                : queryParameters.getRowSelection().getMaxRows().intValue();
				final int size = tmp.size();
				for ( int x = 0; x < size; x++ ) {
					final Object result = tmp.get( x );
					if ( distinction.add( result ) ) {
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
		if ( log.isTraceEnabled() ) {
			log.trace( "iterate: " + getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		if ( translators.length == 0 ) {
			return EmptyIterator.INSTANCE;
		}

		Iterator[] results = null;
		boolean many = translators.length > 1;
		if (many) {
			results = new Iterator[translators.length];
		}

		Iterator result = null;
		for ( int i = 0; i < translators.length; i++ ) {
			result = translators[i].iterate( queryParameters, session );
			if (many) results[i] = result;
		}

		return many ? new JoinedIterator(results) : result;
	}

	public ScrollableResults performScroll(
			QueryParameters queryParameters,
	        SessionImplementor session) throws HibernateException {
		if ( log.isTraceEnabled() ) {
			log.trace( "iterate: " + getSourceQuery() );
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
		if ( log.isTraceEnabled() ) {
			log.trace( "executeUpdate: " + getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		if ( translators.length != 1 ) {
			log.warn( "manipulation query [" + getSourceQuery() + "] resulted in [" + translators.length + "] split queries" );
		}
		int result = 0;
		for ( int i = 0; i < translators.length; i++ ) {
			result += translators[i].executeUpdate( queryParameters, session );
		}
		return result;
	}

	private ParameterMetadata buildParameterMetadata(ParameterTranslations parameterTranslations, String hql) {
		long start = System.currentTimeMillis();
		ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( hql );
		long end = System.currentTimeMillis();
		if ( log.isTraceEnabled() ) {
			log.trace( "HQL param location recognition took " + (end - start) + " mills (" + hql + ")" );
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
		Map namedParamDescriptorMap = new HashMap();
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
}
