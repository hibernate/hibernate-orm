/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.ast;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QueryExecutionRequestException;
import org.hibernate.hql.ParameterTranslations;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.HqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.exec.BasicExecutor;
import org.hibernate.hql.ast.exec.MultiTableDeleteExecutor;
import org.hibernate.hql.ast.exec.MultiTableUpdateExecutor;
import org.hibernate.hql.ast.exec.StatementExecutor;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.hql.ast.tree.InsertStatement;
import org.hibernate.hql.ast.tree.QueryNode;
import org.hibernate.hql.ast.tree.Statement;
import org.hibernate.hql.ast.util.ASTPrinter;
import org.hibernate.hql.ast.util.NodeTraverser;
import org.hibernate.hql.ast.util.ASTUtil;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.Type;
import org.hibernate.util.IdentitySet;
import org.hibernate.util.StringHelper;
import org.hibernate.util.ReflectHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

/**
 * A QueryTranslator that uses an Antlr-based parser.
 *
 * @author Joshua Davis (pgmjsd@sourceforge.net)
 */
public class QueryTranslatorImpl implements FilterTranslator {

	private static final Logger log = LoggerFactory.getLogger( QueryTranslatorImpl.class );
	private static final Logger AST_LOG = LoggerFactory.getLogger( "org.hibernate.hql.ast.AST" );

	private SessionFactoryImplementor factory;

	private final String queryIdentifier;
	private String hql;
	private boolean shallowQuery;
	private Map tokenReplacements;

	private Map enabledFilters; //TODO:this is only needed during compilation .. can we eliminate the instvar?

	private boolean compiled;
	private QueryLoader queryLoader;
	private StatementExecutor statementExecutor;

	private Statement sqlAst;
	private String sql;

	private ParameterTranslations paramTranslations;
	private List collectedParameterSpecifications;


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

	/**
	 * Compile a "normal" query. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param replacements Defined query substitutions.
	 * @param shallow      Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
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
			if ( log.isDebugEnabled() ) {
				log.debug( "compile() : The query is already compiled, skipping..." );
			}
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
			HqlParser parser = parse( true );

			// PHASE 2 : Analyze the HQL AST, and produce an SQL AST.
			HqlSqlWalker w = analyze( parser, collectionRole );

			sqlAst = ( Statement ) w.getAST();

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
				generate( ( QueryNode ) sqlAst );
				queryLoader = new QueryLoader( this, factory, w.getSelectClause() );
			}

			compiled = true;
		}
		catch ( QueryException qe ) {
			qe.setQueryString( hql );
			throw qe;
		}
		catch ( RecognitionException e ) {
			// we do not actually propogate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			if ( log.isTraceEnabled() ) {
				log.trace( "converted antlr.RecognitionException", e );
			}
			throw QuerySyntaxException.convert( e, hql );
		}
		catch ( ANTLRException e ) {
			// we do not actually propogate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			if ( log.isTraceEnabled() ) {
				log.trace( "converted antlr.ANTLRException", e );
			}
			throw new QueryException( e.getMessage(), hql );
		}

		this.enabledFilters = null; //only needed during compilation phase...
	}

	private void generate(AST sqlAst) throws QueryException, RecognitionException {
		if ( sql == null ) {
			SqlGenerator gen = new SqlGenerator(factory);
			gen.statement( sqlAst );
			sql = gen.getSQL();
			if ( log.isDebugEnabled() ) {
				log.debug( "HQL: " + hql );
				log.debug( "SQL: " + sql );
			}
			gen.getParseErrorHandler().throwQueryException();
			collectedParameterSpecifications = gen.getCollectedParameters();
		}
	}

	private HqlSqlWalker analyze(HqlParser parser, String collectionRole) throws QueryException, RecognitionException {
		HqlSqlWalker w = new HqlSqlWalker( this, factory, parser, tokenReplacements, collectionRole );
		AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );

		if ( AST_LOG.isDebugEnabled() ) {
			ASTPrinter printer = new ASTPrinter( SqlTokenTypes.class );
			AST_LOG.debug( printer.showAsString( w.getAST(), "--- SQL AST ---" ) );
		}

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	private HqlParser parse(boolean filter) throws TokenStreamException, RecognitionException {
		// Parse the query string into an HQL AST.
		HqlParser parser = HqlParser.getInstance( hql );
		parser.setFilter( filter );

		if ( log.isDebugEnabled() ) {
			log.debug( "parse() - HQL: " + hql );
		}
		parser.statement();

		AST hqlAst = parser.getAST();

		JavaConstantConverter converter = new JavaConstantConverter();
		NodeTraverser walker = new NodeTraverser( converter );
		walker.traverseDepthFirst( hqlAst );

		showHqlAst( hqlAst );

		parser.getParseErrorHandler().throwQueryException();
		return parser;
	}

	void showHqlAst(AST hqlAst) {
		if ( AST_LOG.isDebugEnabled() ) {
			ASTPrinter printer = new ASTPrinter( HqlTokenTypes.class );
			AST_LOG.debug( printer.showAsString( hqlAst, "--- HQL AST ---" ) );
		}
	}

	private void errorIfDML() throws HibernateException {
		if ( sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for DML operations", hql );
		}
	}

	private void errorIfSelect() throws HibernateException {
		if ( !sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for select queries", hql );
		}
	}

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
	public Type[] getReturnTypes() {
		errorIfDML();
		return getWalker().getReturnTypes();
	}

	public String[] getReturnAliases() {
		errorIfDML();
		return getWalker().getReturnAliases();
	}

	public String[][] getColumnNames() {
		errorIfDML();
		return getWalker().getSelectClause().getColumnNames();
	}

	public Set getQuerySpaces() {
		return getWalker().getQuerySpaces();
	}

	public List list(SessionImplementor session, QueryParameters queryParameters)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		QueryNode query = ( QueryNode ) sqlAst;
		boolean hasLimit = queryParameters.getRowSelection() != null && queryParameters.getRowSelection().definesLimits();
		boolean needsDistincting = ( query.getSelectClause().isDistinct() || hasLimit ) && containsCollectionFetches();

		QueryParameters queryParametersToUse;
		if ( hasLimit && containsCollectionFetches() ) {
			log.warn( "firstResult/maxResults specified with collection fetch; applying in memory!" );
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
						: queryParameters.getRowSelection().getFirstRow().intValue();
			int max = !hasLimit || queryParameters.getRowSelection().getMaxRows() == null
						? -1
						: queryParameters.getRowSelection().getMaxRows().intValue();
			int size = results.size();
			List tmp = new ArrayList();
			IdentitySet distinction = new IdentitySet();
			for ( int i = 0; i < size; i++ ) {
				final Object result = results.get( i );
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
	public Iterator iterate(QueryParameters queryParameters, EventSource session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.iterate( queryParameters, session );
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 */
	public ScrollableResults scroll(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.scroll( queryParameters, session );
	}

	public int executeUpdate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		errorIfSelect();
		return statementExecutor.execute( queryParameters, session );
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 */
	public String getSQLString() {
		return sql;
	}

	public List collectSqlStrings() {
		ArrayList list = new ArrayList();
		if ( isManipulationStatement() ) {
			String[] sqlStatements = statementExecutor.getSqlStatements();
			for ( int i = 0; i < sqlStatements.length; i++ ) {
				list.add( sqlStatements[i] );
			}
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

	public String getQueryString() {
		return hql;
	}

	public Map getEnabledFilters() {
		return enabledFilters;
	}

	public int[] getNamedParameterLocs(String name) {
		return getWalker().getNamedParameterLocations( name );
	}

	public boolean containsCollectionFetches() {
		errorIfDML();
		List collectionFetches = ( ( QueryNode ) sqlAst ).getFromClause().getCollectionFetches();
		return collectionFetches != null && collectionFetches.size() > 0;
	}

	public boolean isManipulationStatement() {
		return sqlAst.needsExecutor();
	}

	public void validateScrollability() throws HibernateException {
		// Impl Note: allows multiple collection fetches as long as the
		// entire fecthed graph still "points back" to a single
		// root entity for return

		errorIfDML();

		QueryNode query = ( QueryNode ) sqlAst;

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
		Iterator itr = query.getSelectClause().getFromElementsForLoad().iterator();
		while ( itr.hasNext() ) {
			// should be the first, but just to be safe...
			final FromElement fromElement = ( FromElement ) itr.next();
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
			String expectedPrimaryOrderSeq = StringHelper.join(
			        ", ",
			        StringHelper.qualify( owner.getTableAlias(), idColNames )
			);
			if (  !primaryOrdering.getText().startsWith( expectedPrimaryOrderSeq ) ) {
				throw new HibernateException( "cannot scroll results with collection fetches which are not ordered primarily by the root entity's PK" );
			}
		}
	}

	private StatementExecutor buildAppropriateStatementExecutor(HqlSqlWalker walker) {
		Statement statement = ( Statement ) walker.getAST();
		if ( walker.getStatementType() == HqlSqlTokenTypes.DELETE ) {
			FromElement fromElement = walker.getFinalFromClause().getFromElement();
			Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
				return new MultiTableDeleteExecutor( walker );
			}
			else {
				return new BasicExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == HqlSqlTokenTypes.UPDATE ) {
			FromElement fromElement = walker.getFinalFromClause().getFromElement();
			Queryable persister = fromElement.getQueryable();
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
			return new BasicExecutor( walker, ( ( InsertStatement ) statement ).getIntoClause().getQueryable() );
		}
		else {
			throw new QueryException( "Unexpected statement type" );
		}
	}

	public ParameterTranslations getParameterTranslations() {
		if ( paramTranslations == null ) {
			paramTranslations = new ParameterTranslationsImpl( getWalker().getParameters() );
//			paramTranslations = new ParameterTranslationsImpl( collectedParameterSpecifications );
		}
		return paramTranslations;
	}

	public List getCollectedParameterSpecifications() {
		return collectedParameterSpecifications;
	}

	public static class JavaConstantConverter implements NodeTraverser.VisitationStrategy {
		private AST dotRoot;
		public void visit(AST node) {
			if ( dotRoot != null ) {
				// we are already processing a dot-structure
				if ( ASTUtil.isSubtreeChild( dotRoot, node ) ) {
					// ignore it...
					return;
				}
				else {
					// we are now at a new tree level
					dotRoot = null;
				}
			}

			if ( dotRoot == null && node.getType() == HqlTokenTypes.DOT ) {
				dotRoot = node;
				handleDotStructure( dotRoot );
			}
		}
		private void handleDotStructure(AST dotStructureRoot) {
			String expression = ASTUtil.getPathText( dotStructureRoot );
			Object constant = ReflectHelper.getConstantValue( expression );
			if ( constant != null ) {
				dotStructureRoot.setFirstChild( null );
				dotStructureRoot.setType( HqlTokenTypes.JAVA_CONSTANT );
				dotStructureRoot.setText( expression );
			}
		}
	}
}
