/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008 Red Hat Inc. or third-party contributors as
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
package org.hibernate.sql.ordering.antlr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import antlr.CommonAST;
import antlr.TokenStream;
import antlr.collections.AST;

import org.jboss.logging.Logger;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.Template;

/**
 * Extension of the Antlr-generated parser for the purpose of adding our custom parsing behavior
 * (semantic analysis, etc).
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentParser extends GeneratedOrderByFragmentParser {
    private static final Logger LOG = Logger.getLogger(OrderByFragmentParser.class.getName());

	private final TranslationContext context;

	private Set<String> columnReferences = new HashSet<String>();

	public OrderByFragmentParser(TokenStream lexer, TranslationContext context) {
		super( lexer );
		super.setASTFactory( new Factory() );
		this.context = context;
	}

	public Set<String> getColumnReferences() {
		return columnReferences;
	}

	@Override
    protected AST quotedIdentifier(AST ident) {
		/*
		 * Semantic action used during recognition of quoted identifiers (quoted column names)
		 */
		final String columnName = context.getDialect().quote( '`' + ident.getText() + '`' );
		columnReferences.add( columnName );
		final String marker = '{' + columnName + '}';
		return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, marker );
	}

	@Override
    protected AST quotedString(AST ident) {
		/*
		 * Semantic action used during recognition of quoted strings (string literals)
		 */
		return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, context.getDialect().quote( ident.getText() ) );
	}

	@Override
	@SuppressWarnings("SimplifiableIfStatement")
	protected boolean isFunctionName(AST ast) {
		/*
		 * Semantic predicate used to determine whether a given AST node represents a function call
		 */

		AST child = ast.getFirstChild();
		// assume it is a function if it has parameters
		if ( child != null && "{param list}".equals( child.getText() ) ) {
			return true;
		}

		// otherwise, in order for this to be a function logically it has to be a function that does not
		// have arguments.  So try to assert that using the registry of known functions
		final SQLFunction function = context.getSqlFunctionRegistry().findSQLFunction( ast.getText() );
		if ( function == null ) {
			// no registered function, so we cannot know for certain
			return false;
		}
		else {
			// if function.hasParenthesesIfNoArguments() is true, then assume the node is not a function
			return ! function.hasParenthesesIfNoArguments();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
    protected AST resolveFunction(AST ast) {
		/*
		 * Semantic action used during recognition of a *known* function
		 */
		AST child = ast.getFirstChild();
		if ( child != null ) {
			assert "{param list}".equals(  child.getText() );
			child = child.getFirstChild();
		}

		final String functionName = ast.getText();
		final SQLFunction function = context.getSqlFunctionRegistry().findSQLFunction( functionName );
		if ( function == null ) {
			String text = functionName;
			if ( child != null ) {
				text += '(';
				while ( child != null ) {
					text += resolveFunctionArgument( child );
					child = child.getNextSibling();
					if ( child != null ) {
						text += ", ";
					}
				}
				text += ')';
			}
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, text );
		}
		else {
			ArrayList expressions = new ArrayList();
			while ( child != null ) {
				expressions.add( resolveFunctionArgument( child ) );
				child = child.getNextSibling();
			}
			final String text = function.render( null, expressions, context.getSessionFactory() );
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, text );
		}
	}

	private String resolveFunctionArgument(AST argumentNode) {
		final String nodeText = argumentNode.getText();
		final String adjustedText;
		if ( nodeText.contains( Template.TEMPLATE ) ) {
			// we have a SQL order-by fragment
			adjustedText = adjustTemplateReferences( nodeText );
		}
		else if ( nodeText.startsWith( "{" ) && nodeText.endsWith( "}" ) ) {
			columnReferences.add( nodeText.substring( 1, nodeText.length() - 1 ) );
			return nodeText;
		}
		else {
			adjustedText = nodeText;
			// because we did not process the node text, we need to attempt to find any column references
			// contained in it.
			// NOTE : uses regex for the time being; we should check the performance of this
			Pattern pattern = Pattern.compile( "\\{(.*)\\}" );
			Matcher matcher = pattern.matcher( adjustedText );
			while ( matcher.find() ) {
				columnReferences.add( matcher.group( 1 ) );
			}
		}
		return adjustedText;
	}

	@Override
    protected AST resolveIdent(AST ident) {
		/*
		 * Semantic action used during recognition of an identifier.  This identifier might be a column name, it might
		 * be a property name.
		 */
		String text = ident.getText();
		SqlValueReference[] sqlValueReferences;
		try {
			sqlValueReferences = context.getColumnMapper().map( text );
		}
		catch( Throwable t ) {
			sqlValueReferences = null;
		}

		if ( sqlValueReferences == null || sqlValueReferences.length == 0 ) {
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, makeColumnReference( text ) );
		}
		else if ( sqlValueReferences.length == 1 ) {
			return processSqlValueReference( sqlValueReferences[0] );
		}
		else {
			final AST root = getASTFactory().create( OrderByTemplateTokenTypes.IDENT_LIST, "{ident list}" );
			for ( SqlValueReference sqlValueReference : sqlValueReferences ) {
				root.addChild( processSqlValueReference( sqlValueReference ) );
			}
			return root;
		}
	}

	private AST processSqlValueReference(SqlValueReference sqlValueReference) {
		if ( ColumnReference.class.isInstance( sqlValueReference ) ) {
			final String columnName = ( (ColumnReference) sqlValueReference ).getColumnName();
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, makeColumnReference( columnName ) );
		}
		else {
			final String formulaFragment = ( (FormulaReference) sqlValueReference ).getFormulaFragment();
			// formulas have already been "adjusted" for aliases by appending Template.TEMPLATE to places
			// where we believe column references are.  Fixing that is beyond scope of this work.  But we need
			// to re-adjust that to use the order-by expectation of wrapping the column names in curly
			// braces (i.e., `{column_name}`).
			final String adjustedText = adjustTemplateReferences( formulaFragment );
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, adjustedText );
		}
	}

	private String makeColumnReference(String text) {
		columnReferences.add( text );
		return "{" + text + "}";
	}

	private static final int TEMPLATE_MARKER_LENGTH = Template.TEMPLATE.length();

	private String adjustTemplateReferences(String template) {
		int templateLength = template.length();
		int startPos = template.indexOf( Template.TEMPLATE );
		while ( startPos < templateLength ) {
			int dotPos = startPos + TEMPLATE_MARKER_LENGTH;

			// from here we need to seek the end of the qualified identifier
			int pos = dotPos + 1;
			while ( pos < templateLength && isValidIdentifierCharacter( template.charAt( pos ) ) ) {
				pos++;
			}

			// At this point we know all 3 points in the template that are needed for replacement.
			// Basically we will be replacing the whole match with the bit following the dot, but will wrap
			// the replacement in curly braces.
			final String columnReference = template.substring( dotPos + 1, pos );
			final String replacement = "{" + columnReference + "}";
			template = template.replace( template.substring( startPos, pos ), replacement );
			columnReferences.add( columnReference );

			// prep for the next seek
			startPos = ( pos - TEMPLATE_MARKER_LENGTH ) + 1;
			templateLength = template.length();
		}

		return template;
	}

	private static boolean isValidIdentifierCharacter(char c) {
		return Character.isLetter( c )
				|| Character.isDigit( c )
				|| '_' == c
				|| '\"' == c;
	}

	@Override
    protected AST postProcessSortSpecification(AST sortSpec) {
		assert SORT_SPEC == sortSpec.getType();
		SortSpecification sortSpecification = ( SortSpecification ) sortSpec;
		AST sortKey = sortSpecification.getSortKey();
		if ( IDENT_LIST == sortKey.getFirstChild().getType() ) {
			AST identList = sortKey.getFirstChild();
			AST ident = identList.getFirstChild();
			AST holder = new CommonAST();
			do {
				holder.addChild(
						createSortSpecification(
								ident,
								sortSpecification.getCollation(),
								sortSpecification.getOrdering()
						)
				);
				ident = ident.getNextSibling();
			} while ( ident != null );
			sortSpec = holder.getFirstChild();
		}
		return sortSpec;
	}

	private SortSpecification createSortSpecification(
			AST ident,
			CollationSpecification collationSpecification,
			OrderingSpecification orderingSpecification) {
		AST sortSpecification = getASTFactory().create( SORT_SPEC, "{{sort specification}}" );
		AST sortKey = getASTFactory().create( SORT_KEY, "{{sort key}}" );
		AST newIdent = getASTFactory().create( ident.getType(), ident.getText() );
		sortKey.setFirstChild( newIdent );
		sortSpecification.setFirstChild( sortKey );
		if ( collationSpecification != null ) {
			sortSpecification.addChild( collationSpecification );
		}
		if ( orderingSpecification != null ) {
			sortSpecification.addChild( orderingSpecification );
		}
		return ( SortSpecification ) sortSpecification;
	}



	// trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private int traceDepth = 0;


	@Override
	public void traceIn(String ruleName) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = StringHelper.repeat( '-', (traceDepth++ * 2) ) + "-> ";
		LOG.trace(prefix + ruleName);
	}

	@Override
	public void traceOut(String ruleName) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = "<-" + StringHelper.repeat( '-', (--traceDepth * 2) ) + " ";
		LOG.trace(prefix + ruleName);
	}

	@Override
	protected void trace(String msg) {
		LOG.trace( msg );
	}
}
