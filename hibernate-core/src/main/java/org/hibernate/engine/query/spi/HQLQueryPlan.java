/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.Filter;
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
import org.hibernate.internal.CoreLogging;
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
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( HQLQueryPlan.class );

    // TODO : keep separate notions of QT[] here for shallow/non-shallow queries...

	private final String sourceQuery;
	private final QueryTranslator[] translators;
	private final String[] sqlStrings;

	private final ParameterMetadata parameterMetadata;
	private final ReturnMetadata returnMetadata;
	private final Set querySpaces;

	private final Set<String> enabledFilterNames;
	private final boolean shallow;

	/**
	* We'll check the trace level only once per instance
	*/
	private final boolean traceEnabled = LOG.isTraceEnabled();

	/**
	 * Constructs a HQLQueryPlan
	 *
	 * @param hql The HQL query
	 * @param shallow Whether the execution is to be shallow or not
	 * @param enabledFilters The enabled filters (we only keep the names)
	 * @param factory The factory
	 */
	public HQLQueryPlan(String hql, boolean shallow, Map<String,Filter> enabledFilters,
			SessionFactoryImplementor factory) {
		this( hql, null, shallow, enabledFilters, factory, null );
	}
	
	public HQLQueryPlan(String hql, boolean shallow, Map<String,Filter> enabledFilters,
			SessionFactoryImplementor factory, EntityGraphQueryHint entityGraphQueryHint) {
		this( hql, null, shallow, enabledFilters, factory, entityGraphQueryHint );
	}

	@SuppressWarnings("unchecked")
	protected HQLQueryPlan(
			String hql,
			String collectionRole,
			boolean shallow,
			Map<String,Filter> enabledFilters,
			SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint) {
		this.sourceQuery = hql;
		this.shallow = shallow;

		final Set<String> copy = new HashSet<String>();
		copy.addAll( enabledFilters.keySet() );
		this.enabledFilterNames = java.util.Collections.unmodifiableSet( copy );

		final String[] concreteQueryStrings = QuerySplitter.concreteQueries( hql, factory );
		final int length = concreteQueryStrings.length;
		this.translators = new QueryTranslator[length];

		final List<String> sqlStringList = new ArrayList<String>();
		final Set<Serializable> combinedQuerySpaces = new HashSet<Serializable>();

		final boolean hasCollectionRole = (collectionRole == null);
		final Map querySubstitutions = factory.getSessionFactoryOptions().getQuerySubstitutions();
		final QueryTranslatorFactory queryTranslatorFactory = factory.getServiceRegistry().getService( QueryTranslatorFactory.class );


		for ( int i=0; i<length; i++ ) {
			if ( hasCollectionRole ) {
				translators[i] = queryTranslatorFactory
						.createQueryTranslator( hql, concreteQueryStrings[i], enabledFilters, factory, entityGraphQueryHint );
				translators[i].compile( querySubstitutions, shallow );
			}
			else {
				translators[i] = queryTranslatorFactory
						.createFilterTranslator( hql, concreteQueryStrings[i], enabledFilters, factory );
				( (FilterTranslator) translators[i] ).compile( collectionRole, querySubstitutions, shallow );
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

	/**
	 * Coordinates the efforts to perform a list across all the included query translators.
	 *
	 * @param queryParameters The query parameters
	 * @param session The session
	 *
	 * @return The query result list
	 *
	 * @throws HibernateException Indicates a problem performing the query
	 */
	@SuppressWarnings("unchecked")
	public List performList(
			QueryParameters queryParameters,
			SessionImplementor session) throws HibernateException {
		if ( traceEnabled ) {
			LOG.tracev( "Find: {0}", getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}

		final RowSelection rowSelection = queryParameters.getRowSelection();
		final boolean hasLimit = rowSelection != null
				&& rowSelection.definesLimits();
		final boolean needsLimit = hasLimit && translators.length > 1;

		final QueryParameters queryParametersToUse;
		if ( needsLimit ) {
			LOG.needsLimit();
			final RowSelection selection = new RowSelection();
			selection.setFetchSize( queryParameters.getRowSelection().getFetchSize() );
			selection.setTimeout( queryParameters.getRowSelection().getTimeout() );
			queryParametersToUse = queryParameters.createCopyUsing( selection );
		}
		else {
			queryParametersToUse = queryParameters;
		}

		final int guessedResultSize = guessResultSize( rowSelection );
		final List combinedResults = new ArrayList( guessedResultSize );
		final IdentitySet distinction = new IdentitySet( guessedResultSize );
		int includedCount = -1;
		translator_loop:
		for ( QueryTranslator translator : translators ) {
			final List tmp = translator.list( session, queryParametersToUse );
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

	/**
	 * If we're able to guess a likely size of the results we can optimize allocation
	 * of our datastructures.
	 * Essentially if we detect the user is not using pagination, we attempt to use the FetchSize
	 * as a reasonable hint. If fetch size is not being set either, it is reasonable to expect
	 * that we're going to have a single hit. In such a case it would be tempting to return a constant
	 * of value one, but that's dangerous as it doesn't scale up appropriately for example
	 * with an ArrayList if the guess is wrong.
	 *
	 * @param rowSelection
	 * @return a reasonable size to use for allocation
	 */
	@SuppressWarnings("UnnecessaryUnboxing")
	private int guessResultSize(RowSelection rowSelection) {
		if ( rowSelection != null ) {
			final int maxReasonableAllocation = rowSelection.getFetchSize() != null ? rowSelection.getFetchSize().intValue() : 100;
			if ( rowSelection.getMaxRows() != null && rowSelection.getMaxRows().intValue() > 0 ) {
				return Math.min( maxReasonableAllocation, rowSelection.getMaxRows().intValue() );
			}
			else if ( rowSelection.getFetchSize() != null && rowSelection.getFetchSize().intValue() > 0 ) {
				return rowSelection.getFetchSize().intValue();
			}
		}
		return 7;//magic number guessed as a reasonable default.
	}

	/**
	 * Coordinates the efforts to perform an iterate across all the included query translators.
	 *
	 * @param queryParameters The query parameters
	 * @param session The session
	 *
	 * @return The query result iterator
	 *
	 * @throws HibernateException Indicates a problem performing the query
	 */
	@SuppressWarnings("unchecked")
	public Iterator performIterate(
			QueryParameters queryParameters,
			EventSource session) throws HibernateException {
		if ( traceEnabled ) {
			LOG.tracev( "Iterate: {0}", getSourceQuery() );
			queryParameters.traceParameters( session.getFactory() );
		}
		if ( translators.length == 0 ) {
			return EmptyIterator.INSTANCE;
		}

		final boolean many = translators.length > 1;
		Iterator[] results = null;
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

		return many ? new JoinedIterator( results ) : result;
	}

	/**
	 * Coordinates the efforts to perform a scroll across all the included query translators.
	 *
	 * @param queryParameters The query parameters
	 * @param session The session
	 *
	 * @return The query result iterator
	 *
	 * @throws HibernateException Indicates a problem performing the query
	 */
	public ScrollableResults performScroll(
			QueryParameters queryParameters,
			SessionImplementor session) throws HibernateException {
		if ( traceEnabled ) {
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

	/**
	 * Coordinates the efforts to perform an execution across all the included query translators.
	 *
	 * @param queryParameters The query parameters
	 * @param session The session
	 *
	 * @return The aggregated "affected row" count
	 *
	 * @throws HibernateException Indicates a problem performing the execution
	 */
	public int performExecuteUpdate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		if ( traceEnabled ) {
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
		final long start = traceEnabled ? System.nanoTime() : 0;
		final ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( hql );

		if ( traceEnabled ) {
			final long end = System.nanoTime();
			LOG.tracev( "HQL param location recognition took {0} nanoseconds ({1})", ( end - start ), hql );
		}

		int ordinalParamCount = parameterTranslations.getOrdinalParameterCount();
		final int[] locations = ArrayHelper.toIntArray( recognizer.getOrdinalParameterLocationList() );
		if ( parameterTranslations.supportsOrdinalParameterMetadata() && locations.length != ordinalParamCount ) {
			throw new HibernateException( "ordinal parameter mismatch" );
		}
		ordinalParamCount = locations.length;

		final OrdinalParameterDescriptor[] ordinalParamDescriptors = new OrdinalParameterDescriptor[ordinalParamCount];
		for ( int i = 1; i <= ordinalParamCount; i++ ) {
			ordinalParamDescriptors[ i - 1 ] = new OrdinalParameterDescriptor(
					i,
					parameterTranslations.supportsOrdinalParameterMetadata()
							? parameterTranslations.getOrdinalParameterExpectedType( i )
							: null,
					locations[ i - 1 ]
			);
		}

		final Map<String, NamedParameterDescriptor> namedParamDescriptorMap = new HashMap<String, NamedParameterDescriptor>();
		final Map<String, ParamLocationRecognizer.NamedParameterDescription> map = recognizer.getNamedParameterDescriptionMap();
		for ( final String name : map.keySet() ) {
			final ParamLocationRecognizer.NamedParameterDescription description = map.get( name );
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

	/**
	 * Access to the underlying translators associated with this query
	 *
	 * @return The translators
	 */
	public QueryTranslator[] getTranslators() {
		final QueryTranslator[] copy = new QueryTranslator[translators.length];
		System.arraycopy( translators, 0, copy, 0, copy.length );
		return copy;
	}

	public Class getDynamicInstantiationResultType() {
		return translators[0].getDynamicInstantiationResultType();
	}

	public boolean isSelect() {
		return !translators[0].isManipulationStatement();
	}
}
