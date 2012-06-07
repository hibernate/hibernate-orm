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
package org.hibernate.hql.internal.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import antlr.RecognitionException;
import antlr.collections.AST;
import org.jboss.logging.Logger;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.SqlGeneratorBase;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.FunctionNode;
import org.hibernate.hql.internal.ast.tree.Node;
import org.hibernate.hql.internal.ast.tree.ParameterContainer;
import org.hibernate.hql.internal.ast.tree.ParameterNode;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.type.Type;

/**
 * Generates SQL by overriding callback methods in the base class, which does
 * the actual SQL AST walking.
 *
 * @author Joshua Davis
 * @author Steve Ebersole
 */
public class SqlGenerator extends SqlGeneratorBase implements ErrorReporter {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SqlGenerator.class.getName());

	public static boolean REGRESSION_STYLE_CROSS_JOINS = false;

	/**
	 * all append invocations on the buf should go through this Output instance variable.
	 * The value of this variable may be temporarily substituted by sql function processing code
	 * to catch generated arguments.
	 * This is because sql function templates need arguments as separate string chunks
	 * that will be assembled into the target dialect-specific function call.
	 */
	private SqlWriter writer = new DefaultWriter();

	private ParseErrorHandler parseErrorHandler;
	private SessionFactoryImplementor sessionFactory;
	private LinkedList<SqlWriter> outputStack = new LinkedList<SqlWriter>();
	private final ASTPrinter printer = new ASTPrinter( SqlTokenTypes.class );
	private List collectedParameters = new ArrayList();


	// handle trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private int traceDepth = 0;

	@Override
	public void traceIn(String ruleName, AST tree) {
		if ( !LOG.isTraceEnabled() ) return;
		if ( inputState.guessing > 0 ) return;
		String prefix = StringHelper.repeat( '-', ( traceDepth++ * 2 ) ) + "-> ";
		String traceText = ruleName + " (" + buildTraceNodeName( tree ) + ")";
		LOG.trace( prefix + traceText );
	}

	private String buildTraceNodeName(AST tree) {
		return tree == null
				? "???"
				: tree.getText() + " [" + printer.getTokenTypeName( tree.getType() ) + "]";
	}

	@Override
	public void traceOut(String ruleName, AST tree) {
		if ( !LOG.isTraceEnabled() ) return;
		if ( inputState.guessing > 0 ) return;
		String prefix = "<-" + StringHelper.repeat( '-', ( --traceDepth * 2 ) ) + " ";
		LOG.trace( prefix + ruleName );
	}

	public List getCollectedParameters() {
		return collectedParameters;
	}

	@Override
    protected void out(String s) {
		writer.clause( s );
	}

	@Override
    protected void out(AST n) {
		if ( n instanceof Node ) {
			out( ( ( Node ) n ).getRenderText( sessionFactory ) );
		}
		else {
			super.out( n );
		}

		if ( n instanceof ParameterNode ) {
			collectedParameters.add( ( ( ParameterNode ) n ).getHqlParameterSpecification() );
		}
		else if ( n instanceof ParameterContainer ) {
			if ( ( ( ParameterContainer ) n ).hasEmbeddedParameters() ) {
				ParameterSpecification[] specifications = ( ( ParameterContainer ) n ).getEmbeddedParameters();
				if ( specifications != null ) {
					collectedParameters.addAll( Arrays.asList( specifications ) );
				}
			}
		}
	}

	@Override
    protected void commaBetweenParameters(String comma) {
		writer.commaBetweenParameters( comma );
	}

	@Override
    public void reportError(RecognitionException e) {
		parseErrorHandler.reportError( e ); // Use the delegate.
	}

	@Override
    public void reportError(String s) {
		parseErrorHandler.reportError( s ); // Use the delegate.
	}

	@Override
    public void reportWarning(String s) {
		parseErrorHandler.reportWarning( s );
	}

	public ParseErrorHandler getParseErrorHandler() {
		return parseErrorHandler;
	}

	public SqlGenerator(SessionFactoryImplementor sfi) {
		super();
		parseErrorHandler = new ErrorCounter();
		sessionFactory = sfi;
	}

	public String getSQL() {
		return getStringBuilder().toString();
	}

	@Override
    protected void optionalSpace() {
		int c = getLastChar();
		switch ( c ) {
			case -1:
				return;
			case ' ':
				return;
			case ')':
				return;
			case '(':
				return;
			default:
				out( " " );
		}
	}

	@Override
    protected void beginFunctionTemplate(AST node, AST nameNode) {
		// NOTE for AGGREGATE both nodes are the same; for METHOD the first is the METHOD, the second is the
		// 		METHOD_NAME
		FunctionNode functionNode = ( FunctionNode ) node;
		SQLFunction sqlFunction = functionNode.getSQLFunction();
		if ( sqlFunction == null ) {
			// if SQLFunction is null we just write the function out as it appears in the hql statement
			super.beginFunctionTemplate( node, nameNode );
		}
		else {
			// this function has a registered SQLFunction -> redirect output and catch the arguments
			outputStack.addFirst( writer );
			writer = new FunctionArguments();
		}
	}

	@Override
    protected void endFunctionTemplate(AST node) {
		FunctionNode functionNode = ( FunctionNode ) node;
		SQLFunction sqlFunction = functionNode.getSQLFunction();
		if ( sqlFunction == null ) {
			super.endFunctionTemplate( node );
		}
		else {
			final Type functionType = functionNode.getFirstArgumentType();
			// this function has a registered SQLFunction -> redirect output and catch the arguments
			FunctionArguments functionArguments = ( FunctionArguments ) writer;
			writer = outputStack.removeFirst();
			out( sqlFunction.render( functionType, functionArguments.getArgs(), sessionFactory ) );
		}
	}

	// --- Inner classes (moved here from sql-gen.g) ---

	/**
	 * Writes SQL fragments.
	 */
	interface SqlWriter {
		void clause(String clause);

		/**
		 * todo remove this hack
		 * The parameter is either ", " or " , ". This is needed to pass sql generating tests as the old
		 * sql generator uses " , " in the WHERE and ", " in SELECT.
		 *
		 * @param comma either " , " or ", "
		 */
		void commaBetweenParameters(String comma);
	}

	/**
	 * SQL function processing code redirects generated SQL output to an instance of this class
	 * which catches function arguments.
	 */
	class FunctionArguments implements SqlWriter {
		private int argInd;
		private final List<String> args = new ArrayList<String>(3);

		public void clause(String clause) {
			if ( argInd == args.size() ) {
				args.add( clause );
			}
			else {
				args.set( argInd, args.get( argInd ) + clause );
			}
		}

		public void commaBetweenParameters(String comma) {
			++argInd;
		}

		public List getArgs() {
			return args;
		}
	}

	/**
	 * The default SQL writer.
	 */
	class DefaultWriter implements SqlWriter {
		public void clause(String clause) {
			getStringBuilder().append( clause );
		}

		public void commaBetweenParameters(String comma) {
			getStringBuilder().append( comma );
		}
	}

    public static void panic() {
		throw new QueryException( "TreeWalker: panic" );
	}

	@Override
    protected void fromFragmentSeparator(AST a) {
		// check two "adjecent" nodes at the top of the from-clause tree
		AST next = a.getNextSibling();
		if ( next == null || !hasText( a ) ) {
			return;
		}

		FromElement left = ( FromElement ) a;
		FromElement right = ( FromElement ) next;

		///////////////////////////////////////////////////////////////////////
		// HACK ALERT !!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// Attempt to work around "ghost" ImpliedFromElements that occasionally
		// show up between the actual things being joined.  This consistently
		// occurs from index nodes (at least against many-to-many).  Not sure
		// if there are other conditions
		//
		// Essentially, look-ahead to the next FromElement that actually
		// writes something to the SQL
		while ( right != null && !hasText( right ) ) {
			right = ( FromElement ) right.getNextSibling();
		}
		if ( right == null ) {
			return;
		}
		///////////////////////////////////////////////////////////////////////

		if ( !hasText( right ) ) {
			return;
		}

		if ( right.getRealOrigin() == left ||
		     ( right.getRealOrigin() != null && right.getRealOrigin() == left.getRealOrigin() ) ) {
			// right represents a joins originating from left; or
			// both right and left reprersent joins originating from the same FromElement
			if ( right.getJoinSequence() != null && right.getJoinSequence().isThetaStyle() ) {
				writeCrossJoinSeparator();
			}
			else {
				out( " " );
			}
		}
		else {
			// these are just two unrelated table references
			writeCrossJoinSeparator();
		}
	}

	private void writeCrossJoinSeparator() {
		if ( REGRESSION_STYLE_CROSS_JOINS ) {
			out( ", " );
		}
		else {
			out( sessionFactory.getDialect().getCrossJoinSeparator() );
		}
	}

	@Override
    protected void nestedFromFragment(AST d, AST parent) {
		// check a set of parent/child nodes in the from-clause tree
		// to determine if a comma is required between them
		if ( d != null && hasText( d ) ) {
			if ( parent != null && hasText( parent ) ) {
				// again, both should be FromElements
				FromElement left = ( FromElement ) parent;
				FromElement right = ( FromElement ) d;
				if ( right.getRealOrigin() == left ) {
					// right represents a joins originating from left...
					if ( right.getJoinSequence() != null && right.getJoinSequence().isThetaStyle() ) {
						out( ", " );
					}
					else {
						out( " " );
					}
				}
				else {
					// not so sure this is even valid subtree.  but if it was, it'd
					// represent two unrelated table references...
					out( ", " );
				}
			}
			out( d );
		}
	}
}
