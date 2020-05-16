/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.exec.BasicExecutor;
import org.hibernate.hql.internal.ast.exec.DeleteExecutor;
import org.hibernate.hql.internal.ast.exec.MultiTableDeleteExecutor;
import org.hibernate.hql.internal.ast.exec.MultiTableUpdateExecutor;
import org.hibernate.hql.internal.ast.exec.StatementExecutor;
import org.hibernate.hql.internal.ast.tree.AggregatedSelectExpression;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.InsertStatement;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.Statement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.NodeTraverser;
import org.hibernate.hql.internal.ast.util.TokenPrinters;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.ParameterTranslations;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

/**
 * A QueryTranslator that uses an Antlr-based parser.
 *
 * @author Joshua Davis (pgmjsd@sourceforge.net)
 */
public class QueryTranslatorImpl implements FilterTranslator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			QueryTranslatorImpl.class.getName()
	);

	private SessionFactoryImplementor factory;

	private final String queryIdentifier;
	private String hql;
	private boolean shallowQuery;
	private Map tokenReplacements;

	//TODO:this is only needed during compilation .. can we eliminate the instvar?
	private Map enabledFilters;

	private boolean compiled;
	private QueryLoader queryLoader;
	private StatementExecutor statementExecutor;

	private Statement sqlAst;
	private String sql;

	private ParameterTranslations paramTranslations;
	private List<ParameterSpecification> collectedParameterSpecifications;

	private EntityGraphQueryHint entityGraphQueryHint;


	/**
	 * Creates a new AST-based query translator.
	 *
	 * @param queryIdentifier The query-identifier (used in stats collection)
	 * @param query The hql query to translate
	 * @param enabledFilters Currently enabled filters
	 * @param factory The session factory constructing this translator instance.
	 */
	public QueryTranslatorImpl(
			String queryIdentifier,
			String query,
			Map enabledFilters,
			SessionFactoryImplementor factory) {
		this.queryIdentifier = queryIdentifier;
		this.hql = query;
		this.compiled = false;
		this.shallowQuery = false;
		this.enabledFilters = enabledFilters;
		this.factory = factory;
	}

	public QueryTranslatorImpl(
			String queryIdentifier,
			String query,
			Map enabledFilters,
			SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint) {
		this( queryIdentifier, query, enabledFilters, factory );
		this.entityGraphQueryHint = entityGraphQueryHint;
	}

	/**
	 * Compile a "normal" query. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param replacements Defined query substitutions.
	 * @param shallow      Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	@Override
	public void compile(
			Map replacements,
			boolean shallow) throws QueryException, MappingException {
		doCompile( replacements, shallow, null );
	}

	/**
	 * Compile a filter. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param collectionRole the role name of the collection used as the basis for the filter.
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	@Override
	public void compile(
			String collectionRole,
			Map replacements,
			boolean shallow) throws QueryException, MappingException {
		doCompile( replacements, shallow, collectionRole );
	}

	/**
	 * Performs both filter and non-filter compiling.
	 *
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @param collectionRole the role name of the collection used as the basis for the filter, NULL if this
	 *                       is not a filter.
	 */
	private synchronized void doCompile(Map replacements, boolean shallow, String collectionRole) {
		// If the query is already compiled, skip the compilation.
		if ( compiled ) {
			LOG.debug( "compile() : The query is already compiled, skipping..." );
			return;
		}

		// Remember the parameters for the compilation.
		this.tokenReplacements = replacements;
		if ( tokenReplacements == null ) {
			tokenReplacements = new HashMap();
		}
		this.shallowQuery = shallow;

		try {
			// PHASE 1 : Parse the HQL into an AST.
			final HqlParser parser = parse( true );

			// PHASE 2 : Analyze the HQL AST, and produce an SQL AST.
			final HqlSqlWalker w = analyze( parser, collectionRole );

			sqlAst = (Statement) w.getAST();

			// at some point the generate phase needs to be moved out of here,
			// because a single object-level DML might spawn multiple SQL DML
			// command executions.
			//
			// Possible to just move the sql generation for dml stuff, but for
			// consistency-sake probably best to just move responsiblity for
			// the generation phase completely into the delegates
			// (QueryLoader/StatementExecutor) themselves.  Also, not sure why
			// QueryLoader currently even has a dependency on this at all; does
			// it need it?  Ideally like to see the walker itself given to the delegates directly...

			if ( sqlAst.needsExecutor() ) {
				statementExecutor = buildAppropriateStatementExecutor( w );
			}
			else {
				// PHASE 3 : Generate the SQL.
				generate( (QueryNode) sqlAst );
				queryLoader = createQueryLoader( w, factory );
			}

			compiled = true;
		}
		catch ( QueryException qe ) {
			if ( qe.getQueryString() == null ) {
				throw qe.wrapWithQueryString( hql );
			}
			else {
				throw qe;
			}
		}
		catch ( RecognitionException e ) {
			// we do not actually propagate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			LOG.trace( "Converted antlr.RecognitionException", e );
			throw QuerySyntaxException.convert( e, hql );
		}
		catch ( ANTLRException e ) {
			// we do not actually propagate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			LOG.trace( "Converted antlr.ANTLRException", e );
			throw new QueryException( e.getMessage(), hql );
		}
		catch ( IllegalArgumentException e ) {
			// translate this into QueryException
			LOG.trace( "Converted IllegalArgumentException", e );
			throw new QueryException( e.getMessage(), hql );
		}

		//only needed during compilation phase...
		this.enabledFilters = null;
	}

	protected QueryLoader createQueryLoader(HqlSqlWalker w, SessionFactoryImplementor factory) {
		return new QueryLoader( this, factory, w.getSelectClause() );
	}

	private void generate(AST sqlAst) throws QueryException, RecognitionException {
		if ( sql == null ) {
			final SqlGenerator gen = new SqlGenerator( factory );
			gen.statement( sqlAst );
			sql = gen.getSQL();
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "HQL: %s", hql );
				LOG.debugf( "SQL: %s", sql );
			}
			gen.getParseErrorHandler().throwQueryException();
			if ( collectedParameterSpecifications == null ) {
				collectedParameterSpecifications = gen.getCollectedParameters();
			}
			else {
				collectedParameterSpecifications.addAll( gen.getCollectedParameters() );
			}
		}
	}

	private HqlSqlWalker analyze(HqlParser parser, String collectionRole) throws QueryException, RecognitionException {
		final HqlSqlWalker w = new HqlSqlWalker( this, factory, parser, tokenReplacements, collectionRole );
		final AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );

		if ( LOG.isDebugEnabled() ) {
			LOG.debug( TokenPrinters.SQL_TOKEN_PRINTER.showAsString( w.getAST(), "--- SQL AST ---" ) );
		}

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	private HqlParser parse(boolean filter) throws TokenStreamException {
		// Parse the query string into an HQL AST.
		final HqlParser parser = HqlParser.getInstance( hql );
		parser.setFilter( filter );

		LOG.debugf( "parse() - HQL: %s", hql );
		try {
			parser.statement();
		}
		catch (RecognitionException e) {
			throw new HibernateException( "Unexpected error parsing HQL", e );
		}

		final AST hqlAst = parser.getAST();
		parser.getParseErrorHandler().throwQueryException();

		final NodeTraverser walker = new NodeTraverser( new JavaConstantConverter( factory ) );
		walker.traverseDepthFirst( hqlAst );

		showHqlAst( hqlAst );

		return parser;
	}

	void showHqlAst(AST hqlAst) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( TokenPrinters.HQL_TOKEN_PRINTER.showAsString( hqlAst, "--- HQL AST ---" ) );
		}
	}

	protected void errorIfDML() throws HibernateException {
		if ( sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for DML operations", hql );
		}
	}

	private void errorIfSelect() throws HibernateException {
		if ( !sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for select queries", hql );
		}
	}
	@Override
	public String getQueryIdentifier() {
		return queryIdentifier;
	}

	public Statement getSqlAST() {
		return sqlAst;
	}

	private HqlSqlWalker getWalker() {
		return sqlAst.getWalker();
	}

	/**
	 * Types of the return values of an <tt>iterate()</tt> style query.
	 *
	 * @return an array of <tt>Type</tt>s.
	 */
	@Override
	public Type[] getReturnTypes() {
		errorIfDML();
		return getWalker().getReturnTypes();
	}
	@Override
	public String[] getReturnAliases() {
		errorIfDML();
		return getWalker().getReturnAliases();
	}
	@Override
	public String[][] getColumnNames() {
		errorIfDML();
		return getWalker().getSelectClause().getColumnNames();
	}
	@Override
	public Set<Serializable> getQuerySpaces() {
		return getWalker().getQuerySpaces();
	}

	@Override
	public List list(SharedSessionContractImplementor session, QueryParameters queryParameters)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();

		final QueryNode query = (QueryNode) sqlAst;
		final boolean hasLimit = queryParameters.getRowSelection() != null && queryParameters.getRowSelection().definesLimits();
		final boolean needsDistincting = (
				query.getSelectClause().isDistinct() ||
				getEntityGraphQueryHint() != null ||
				hasLimit )
		&& containsCollectionFetches();

		QueryParameters queryParametersToUse;
		if ( hasLimit && containsCollectionFetches() ) {
			boolean fail = session.getFactory().getSessionFactoryOptions().isFailOnPaginationOverCollectionFetchEnabled();
			if (fail) {
				throw new HibernateException("firstResult/maxResults specified with collection fetch. " +
						"In memory pagination was about to be applied. " +
						"Failing because 'Fail on pagination over collection fetch' is enabled.");
			}
			else {
				LOG.firstOrMaxResultsSpecifiedWithCollectionFetch();
			}
			RowSelection selection = new RowSelection();
			selection.setFetchSize( queryParameters.getRowSelection().getFetchSize() );
			selection.setTimeout( queryParameters.getRowSelection().getTimeout() );
			queryParametersToUse = queryParameters.createCopyUsing( selection );
		}
		else {
			queryParametersToUse = queryParameters;
		}

		List results = queryLoader.list( session, queryParametersToUse );

		if ( needsDistincting ) {
			int includedCount = -1;
			// NOTE : firstRow is zero-based
			int first = !hasLimit || queryParameters.getRowSelection().getFirstRow() == null
						? 0
						: queryParameters.getRowSelection().getFirstRow();
			int max = !hasLimit || queryParameters.getRowSelection().getMaxRows() == null
						? -1
						: queryParameters.getRowSelection().getMaxRows();
			List tmp = new ArrayList();
			IdentitySet distinction = new IdentitySet();
			for ( final Object result : results ) {
				if ( !distinction.add( result ) ) {
					continue;
				}
				includedCount++;
				if ( includedCount < first ) {
					continue;
				}
				tmp.add( result );
				// NOTE : ( max - 1 ) because first is zero-based while max is not...
				if ( max >= 0 && ( includedCount - first ) >= ( max - 1 ) ) {
					break;
				}
			}
			results = tmp;
		}

		return results;
	}

	/**
	 * Return the query results as an iterator
	 */
	@Override
	public Iterator iterate(QueryParameters queryParameters, EventSource session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.iterate( queryParameters, session );
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 */
	@Override
	public ScrollableResultsImplementor scroll(QueryParameters queryParameters, SharedSessionContractImplementor session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.scroll( queryParameters, session );
	}
	@Override
	public int executeUpdate(QueryParameters queryParameters, SharedSessionContractImplementor session)
			throws HibernateException {
		errorIfSelect();
		return statementExecutor.execute( queryParameters, session );
	}

	/**
	 * The SQl statements used for an update query.
	 * Throws exception if the query is a SELECT.
	 * @see #getSQLString()
	 * @throws QueryExecutionRequestException for select queries.
	 * @return the sql queries used for the update
	 */
	protected String[] getSqlStatements() {
		errorIfSelect();
		return statementExecutor.getSqlStatements();
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 */
	@Override
	public String getSQLString() {
		return sql;
	}
	@Override
	public List<String> collectSqlStrings() {
		ArrayList<String> list = new ArrayList<String>();
		if ( isManipulationStatement() ) {
			String[] sqlStatements = statementExecutor.getSqlStatements();
			Collections.addAll( list, sqlStatements );
		}
		else {
			list.add( sql );
		}
		return list;
	}

	// -- Package local methods for the QueryLoader delegate --

	public boolean isShallowQuery() {
		return shallowQuery;
	}
	@Override
	public String getQueryString() {
		return hql;
	}
	@Override
	public Map getEnabledFilters() {
		return enabledFilters;
	}

	public int[] getNamedParameterLocs(String name) {
		return getWalker().getNamedParameterLocations( name );
	}
	@Override
	public boolean containsCollectionFetches() {
		errorIfDML();
		List collectionFetches = ( (QueryNode) sqlAst ).getFromClause().getCollectionFetches();
		return collectionFetches != null && collectionFetches.size() > 0;
	}
	@Override
	public boolean isManipulationStatement() {
		return sqlAst.needsExecutor();
	}
	@Override
	public boolean isUpdateStatement() {
		return SqlTokenTypes.UPDATE == sqlAst.getStatementType();
	}
	@Override
	public List<String> getPrimaryFromClauseTables() {
		return (List<String>) sqlAst.getWalker()
				.getFinalFromClause()
				.getFromElements()
				.stream()
				.map( elem -> ((FromElement) elem).getTableName() ).
				collect( Collectors.toList() );
	}

	@Override
	public void validateScrollability() throws HibernateException {
		// Impl Note: allows multiple collection fetches as long as the
		// entire fecthed graph still "points back" to a single
		// root entity for return

		errorIfDML();

		final QueryNode query = (QueryNode) sqlAst;

		// If there are no collection fetches, then no further checks are needed
		List collectionFetches = query.getFromClause().getCollectionFetches();
		if ( collectionFetches.isEmpty() ) {
			return;
		}

		// A shallow query is ok (although technically there should be no fetching here...)
		if ( isShallowQuery() ) {
			return;
		}

		// Otherwise, we have a non-scalar select with defined collection fetch(es).
		// Make sure that there is only a single root entity in the return (no tuples)
		if ( getReturnTypes().length > 1 ) {
			throw new HibernateException( "cannot scroll with collection fetches and returned tuples" );
		}

		FromElement owner = null;
		for ( Object o : query.getSelectClause().getFromElementsForLoad() ) {
			// should be the first, but just to be safe...
			final FromElement fromElement = (FromElement) o;
			if ( fromElement.getOrigin() == null ) {
				owner = fromElement;
				break;
			}
		}

		if ( owner == null ) {
			throw new HibernateException( "unable to locate collection fetch(es) owner for scrollability checks" );
		}

		// This is not strictly true.  We actually just need to make sure that
		// it is ordered by root-entity PK and that that order-by comes before
		// any non-root-entity ordering...

		AST primaryOrdering = query.getOrderByClause().getFirstChild();
		if ( primaryOrdering != null ) {
			// TODO : this is a bit dodgy, come up with a better way to check this (plus see above comment)
			String [] idColNames = owner.getQueryable().getIdentifierColumnNames();
			String expectedPrimaryOrderSeq = String.join(
					", ",
					StringHelper.qualify( owner.getTableAlias(), idColNames )
			);
			if (  !primaryOrdering.getText().startsWith( expectedPrimaryOrderSeq ) ) {
				throw new HibernateException( "cannot scroll results with collection fetches which are not ordered primarily by the root entity's PK" );
			}
		}
	}

	private StatementExecutor buildAppropriateStatementExecutor(HqlSqlWalker walker) {
		final Statement statement = (Statement) walker.getAST();
		if ( walker.getStatementType() == HqlSqlTokenTypes.DELETE ) {
			final FromElement fromElement = walker.getFinalFromClause().getFromElement();
			final Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
				return new MultiTableDeleteExecutor( walker );
			}
			else {
				return new DeleteExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == HqlSqlTokenTypes.UPDATE ) {
			final FromElement fromElement = walker.getFinalFromClause().getFromElement();
			final Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
				// even here, if only properties mapped to the "base table" are referenced
				// in the set and where clauses, this could be handled by the BasicDelegate.
				// TODO : decide if it is better performance-wise to doAfterTransactionCompletion that check, or to simply use the MultiTableUpdateDelegate
				return new MultiTableUpdateExecutor( walker );
			}
			else {
				return new BasicExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == HqlSqlTokenTypes.INSERT ) {
			return new BasicExecutor( walker, ( (InsertStatement) statement ).getIntoClause().getQueryable() );
		}
		else {
			throw new QueryException( "Unexpected statement type" );
		}
	}
	@Override
	public ParameterTranslations getParameterTranslations() {
		if ( paramTranslations == null ) {
			paramTranslations = new ParameterTranslationsImpl( getWalker().getParameterSpecs() );
		}
		return paramTranslations;
	}

	public List<ParameterSpecification> getCollectedParameterSpecifications() {
		return collectedParameterSpecifications;
	}

	@Override
	public Class getDynamicInstantiationResultType() {
		AggregatedSelectExpression aggregation = queryLoader.getAggregatedSelectExpression();
		return aggregation == null ? null : aggregation.getAggregationResultType();
	}

	public static class JavaConstantConverter implements NodeTraverser.VisitationStrategy {
		private final SessionFactoryImplementor factory;
		private AST dotRoot;

		public JavaConstantConverter(SessionFactoryImplementor factory) {
			this.factory = factory;
		}

		@Override
		public void visit(AST node) {
			if ( dotRoot != null ) {
				// we are already processing a dot-structure
				if ( ASTUtil.isSubtreeChild( dotRoot, node ) ) {
					return;
				}
				// we are now at a new tree level
				dotRoot = null;
			}

			if ( node.getType() == HqlTokenTypes.DOT ) {
				dotRoot = node;
				handleDotStructure( dotRoot );
			}
		}
		private void handleDotStructure(AST dotStructureRoot) {
			final String expression = ASTUtil.getPathText( dotStructureRoot );
			final Object constant = ReflectHelper.getConstantValue( expression, factory );
			if ( constant != null ) {
				dotStructureRoot.setFirstChild( null );
				dotStructureRoot.setType( HqlTokenTypes.JAVA_CONSTANT );
				dotStructureRoot.setText( expression );
			}
		}
	}

	public EntityGraphQueryHint getEntityGraphQueryHint() {
		return entityGraphQueryHint;
	}

	public void setEntityGraphQueryHint(EntityGraphQueryHint entityGraphQueryHint) {
		this.entityGraphQueryHint = entityGraphQueryHint;
	}
}
