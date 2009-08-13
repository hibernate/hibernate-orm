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
package org.hibernate.sql.ordering.antlr;

import java.util.ArrayList;

import antlr.TokenStream;
import antlr.CommonAST;
import antlr.collections.AST;

import org.hibernate.sql.Template;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.util.StringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the Antlr-generated parser for the purpose of adding our custom parsing behavior.
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentParser extends GeneratedOrderByFragmentParser {
	private static final Logger log = LoggerFactory.getLogger( OrderByFragmentParser.class );

	private final TranslationContext context;

	public OrderByFragmentParser(TokenStream lexer, TranslationContext context) {
		super( lexer );
		super.setASTFactory( new Factory() );
		this.context = context;
	}


	// handle trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private int traceDepth = 0;


	public void traceIn(String ruleName) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = StringHelper.repeat( '-', (traceDepth++ * 2) ) + "-> ";
		log.trace( prefix + ruleName );
	}

	public void traceOut(String ruleName) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = "<-" + StringHelper.repeat( '-', (--traceDepth * 2) ) + " ";
		log.trace( prefix + ruleName );
	}

	/**
	 * {@inheritDoc}
	 */
	protected void trace(String msg) {
		log.trace( msg );
	}

	/**
	 * {@inheritDoc}
	 */
	protected AST quotedIdentifier(AST ident) {
		return getASTFactory().create(
				OrderByTemplateTokenTypes.IDENT,
				Template.TEMPLATE + "." + context.getDialect().quote( '`' + ident.getText() + '`' )
		);
	}

	/**
	 * {@inheritDoc}
	 */
	protected AST quotedString(AST ident) {
		return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, context.getDialect().quote( ident.getText() ) );
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isFunctionName(AST ast) {
		return context.getSqlFunctionRegistry().hasFunction( ast.getText() );
	}

	/**
	 * {@inheritDoc}
	 */
	protected AST resolveFunction(AST ast) {
		AST child = ast.getFirstChild();
		assert "{param list}".equals(  child.getText() );
		child = child.getFirstChild();

		final String functionName = ast.getText();
		final SQLFunction function = context.getSqlFunctionRegistry().findSQLFunction( functionName );
		if ( function == null ) {
			String text = functionName;
			if ( child != null ) {
				text += '(';
				while ( child != null ) {
					text += child.getText();
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
				expressions.add( child.getText() );
				child = child.getNextSibling();
			}
			final String text = function.render( expressions, context.getSessionFactory() );
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, text );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected AST resolveIdent(AST ident) {
		String text = ident.getText();
		String[] replacements;
		try {
			replacements = context.getColumnMapper().map( text );
		}
		catch( Throwable t ) {
			replacements = null;
		}

		if ( replacements == null || replacements.length == 0 ) {
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, Template.TEMPLATE + "." + text );
		}
		else if ( replacements.length == 1 ) {
			return getASTFactory().create( OrderByTemplateTokenTypes.IDENT, Template.TEMPLATE + "." + replacements[0] );
		}
		else {
			final AST root = getASTFactory().create( OrderByTemplateTokenTypes.IDENT_LIST, "{ident list}" );
			for ( int i = 0; i < replacements.length; i++ ) {
				final String identText = Template.TEMPLATE + '.' + replacements[i];
				root.addChild( getASTFactory().create( OrderByTemplateTokenTypes.IDENT, identText ) );
			}
			return root;
		}
	}

	/**
	 * {@inheritDoc}
	 */
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
}
