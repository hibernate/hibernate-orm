/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Delegate for handling function "templates".
 *
 * @author Steve Ebersole
 */
public class PatternRenderer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PatternRenderer.class );

	private final String[] chunks;
	private final int[] paramIndexes;
	private final int varargParam;
	private final int maxParamIndex;
	private final SqlAstNodeRenderingMode[] argumentRenderingModes;

	public PatternRenderer(String pattern) {
		this( pattern, SqlAstNodeRenderingMode.DEFAULT );
	}

	/**
	 * Constructs a template renderer
	 *
	 * @param pattern The template
	 * @param argumentRenderingMode The rendering mode for arguments
	 */
	public PatternRenderer(String pattern, SqlAstNodeRenderingMode argumentRenderingMode) {
		this( pattern, new SqlAstNodeRenderingMode[] { argumentRenderingMode } );
	}

	/**
	 * Constructs a template renderer
	 *
	 * @param pattern The template
	 * @param argumentRenderingModes The rendering modes for arguments
	 */
	public PatternRenderer(String pattern, SqlAstNodeRenderingMode[] argumentRenderingModes) {
		final List<String> chunkList = new ArrayList<>();
		final List<Integer> paramList = new ArrayList<>();
		final StringBuilder chunk = new StringBuilder( 10 );
		final StringBuilder index = new StringBuilder( 2 );

		int vararg = -1;
		int max = 0;

		int i = 0;
		final int len = pattern.length();
		while ( i < len ) {
			char c = pattern.charAt( i );
			if ( c == '?' ) {
				chunkList.add( chunk.toString() );
				chunk.setLength(0);

				while ( ++i < pattern.length() ) {
					c = pattern.charAt( i );
					if ( Character.isDigit( c ) ) {
						index.append( c );
					}
					else if ( c == '.' ) {
						index.append( c );
					}
					else if ( c  == '?' ) {
						i--;
						break;
					}
					else {
						chunk.append( c );
						break;
					}
				}

				if ( index.toString().endsWith("...") ) {
					vararg = paramList.size();
				}
				else {
					int paramNumber = Integer.parseInt( index.toString() );
					paramList.add( paramNumber );
					index.setLength(0);
					if ( paramNumber > max ) {
						max = paramNumber;
					}
				}
			}
			else {
				chunk.append( c );
			}
			i++;
		}

		if ( chunk.length() > 0 ) {
			chunkList.add( chunk.toString() );
		}

		this.varargParam = vararg;
		this.maxParamIndex = max;

		this.chunks = chunkList.toArray( new String[chunkList.size()] );
		int[] paramIndexes = new int[paramList.size()];
		for ( i = 0; i < paramIndexes.length; ++i ) {
			paramIndexes[i] = paramList.get( i );
		}
		this.paramIndexes = paramIndexes;
		this.argumentRenderingModes = argumentRenderingModes;
	}

	public boolean hasVarargs() {
		return varargParam >= 0;
	}

	public int getParamCount() {
		return maxParamIndex;
	}

	/**
	 * The rendering code.
	 *
	 * @param sqlAppender Target for appending
	 * @param args The arguments to inject into the template
	 */
	@SuppressWarnings("unused")
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, args, null, Collections.emptyList(), translator );
	}

	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			Predicate filter,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, args, filter, Collections.emptyList(), null, null, translator );
	}

	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			Predicate filter,
			List<SortSpecification> withinGroup,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, args, filter, withinGroup, null, null, translator );
	}

	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, args, filter, Collections.emptyList(), respectNulls, fromFirst, translator );
	}

	private void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			Predicate filter,
			List<SortSpecification> withinGroup,
			Boolean respectNulls,
			Boolean fromFirst,
			SqlAstTranslator<?> translator) {
		final int numberOfArguments = args.size();
		final boolean caseWrapper = filter != null && !translator.getSessionFactory().getJdbcServices().getDialect().supportsFilterClause();
		if ( numberOfArguments < maxParamIndex ) {
			LOG.missingArguments( maxParamIndex, numberOfArguments );
		}

		for ( int i = 0; i < chunks.length; i++ ) {
			if ( i == varargParam ) {
				final SqlAstNodeRenderingMode argumentRenderingMode = getArgumentRenderingMode(varargParam - 1);
				for ( int j = i; j < numberOfArguments; j++ ) {
					final SqlAstNode arg = args.get( j );
					if ( arg != null ) {
						sqlAppender.appendSql( chunks[i] );
						if ( caseWrapper && !( arg instanceof Distinct ) && !( arg instanceof Star ) ) {
							translator.getCurrentClauseStack().push( Clause.WHERE );
							sqlAppender.appendSql( "case when " );
							filter.accept( translator );
							translator.getCurrentClauseStack().pop();
							sqlAppender.appendSql( " then " );
							translator.render( arg, argumentRenderingMode );
							sqlAppender.appendSql( " else null end" );
						}
						else {
							translator.render( arg, argumentRenderingMode );
						}
					}
				}
			}
			else if ( i < paramIndexes.length ) {
				final int index = paramIndexes[i] - 1;
				final SqlAstNode arg = index < numberOfArguments ? args.get( index ) : null;
				if ( arg != null || i == 0 ) {
					sqlAppender.appendSql( chunks[i] );
				}
				if ( arg != null ) {
					if ( caseWrapper && !( arg instanceof Distinct ) && !( arg instanceof Star ) ) {
						translator.getCurrentClauseStack().push( Clause.WHERE );
						sqlAppender.appendSql( "case when " );
						filter.accept( translator );
						translator.getCurrentClauseStack().pop();
						sqlAppender.appendSql( " then " );
						translator.render( arg, getArgumentRenderingMode(index) );
						sqlAppender.appendSql( " else null end" );
					}
					else {
						translator.render( arg, getArgumentRenderingMode(index) );
					}
				}
			}
			else {
				sqlAppender.appendSql( chunks[i] );
			}
		}

		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " within group (order by" );
			translator.render( withinGroup.get( 0 ), getArgumentRenderingMode( 0 ) );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( SqlAppender.COMMA_SEPARATOR_CHAR );
				translator.render( withinGroup.get( 0 ), getArgumentRenderingMode( 0 ) );
			}
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}

		if ( fromFirst != null ) {
			if ( fromFirst ) {
				sqlAppender.appendSql( " from first" );
			}
			else {
				sqlAppender.appendSql( " from last" );
			}
		}
		if ( respectNulls != null ) {
			if ( respectNulls ) {
				sqlAppender.appendSql( " respect nulls" );
			}
			else {
				sqlAppender.appendSql( " ignore nulls" );
			}
		}

		if ( filter != null && !caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

	private SqlAstNodeRenderingMode getArgumentRenderingMode(int index) {
		if ( index < argumentRenderingModes.length ) {
			return argumentRenderingModes[index];
		}
		return argumentRenderingModes[argumentRenderingModes.length - 1];
	}
}
