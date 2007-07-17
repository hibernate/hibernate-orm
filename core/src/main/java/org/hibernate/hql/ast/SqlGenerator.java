// $Id: SqlGenerator.java 10060 2006-06-28 02:53:39Z steve.ebersole@jboss.com $
package org.hibernate.hql.ast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import antlr.RecognitionException;
import antlr.collections.AST;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.antlr.SqlGeneratorBase;
import org.hibernate.hql.ast.tree.MethodNode;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.hql.ast.tree.Node;

/**
 * Generates SQL by overriding callback methods in the base class, which does
 * the actual SQL AST walking.
 *
 * @author Joshua Davis
 * @author Steve Ebersole
 */
public class SqlGenerator extends SqlGeneratorBase implements ErrorReporter {
	/**
	 * Handles parser errors.
	 */
	private ParseErrorHandler parseErrorHandler;

	/**
	 * all append invocations on the buf should go through this Output instance variable.
	 * The value of this variable may be temporarily substitued by sql function processing code
	 * to catch generated arguments.
	 * This is because sql function templates need arguments as seperate string chunks
	 * that will be assembled into the target dialect-specific function call.
	 */
	private SqlWriter writer = new DefaultWriter();

	private SessionFactoryImplementor sessionFactory;

	private LinkedList outputStack = new LinkedList();

	protected void out(String s) {
		writer.clause( s );
	}

	protected void out(AST n) {
		if ( n instanceof Node ) {
			out( ( ( Node ) n ).getRenderText( sessionFactory ) );
		}
		else {
			super.out( n );
		}
	}

	protected void commaBetweenParameters(String comma) {
		writer.commaBetweenParameters( comma );
	}

	public void reportError(RecognitionException e) {
		parseErrorHandler.reportError( e ); // Use the delegate.
	}

	public void reportError(String s) {
		parseErrorHandler.reportError( s ); // Use the delegate.
	}

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
		return getStringBuffer().toString();
	}

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

	protected void beginFunctionTemplate(AST m, AST i) {
		MethodNode methodNode = ( MethodNode ) m;
		SQLFunction template = methodNode.getSQLFunction();
		if ( template == null ) {
			// if template is null we just write the function out as it appears in the hql statement
			super.beginFunctionTemplate( m, i );
		}
		else {
			// this function has a template -> redirect output and catch the arguments
			outputStack.addFirst( writer );
			writer = new FunctionArguments();
		}
	}

	protected void endFunctionTemplate(AST m) {
		MethodNode methodNode = ( MethodNode ) m;
		SQLFunction template = methodNode.getSQLFunction();
		if ( template == null ) {
			super.endFunctionTemplate( m );
		}
		else {
			// this function has a template -> restore output, apply the template and write the result out
			FunctionArguments functionArguments = ( FunctionArguments ) writer;   // TODO: Downcast to avoid using an interface?  Yuck.
			writer = ( SqlWriter ) outputStack.removeFirst();
			out( template.render( functionArguments.getArgs(), sessionFactory ) );
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
		private final List args = new ArrayList( 3 );

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
			getStringBuffer().append( clause );
		}

		public void commaBetweenParameters(String comma) {
			getStringBuffer().append( comma );
		}
	}

    public static void panic() {
		throw new QueryException( "TreeWalker: panic" );
	}

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
				out( ", " );
			}
			else {
				out( " " );
			}
		}
		else {
			// these are just two unrelated table references
			out( ", " );
		}
	}

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
