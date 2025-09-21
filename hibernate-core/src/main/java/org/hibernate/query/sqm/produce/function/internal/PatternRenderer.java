/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function.internal;

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
import java.util.List;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor.filterClauseSupported;

/**
 * Delegate for handling function "templates".
 *
 * @author Steve Ebersole
 */
public class PatternRenderer {

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
					if ( isDigit( c ) ) {
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
					final int paramNumber = parameterIndex( pattern, index.toString() );
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

		if ( !chunk.isEmpty() ) {
			chunkList.add( chunk.toString() );
		}

		this.varargParam = vararg;
		this.maxParamIndex = max;

		this.chunks = chunkList.toArray( EMPTY_STRING_ARRAY );
		int[] paramIndexes = new int[paramList.size()];
		for ( i = 0; i < paramIndexes.length; ++i ) {
			paramIndexes[i] = paramList.get( i );
		}
		this.paramIndexes = paramIndexes;
		this.argumentRenderingModes = argumentRenderingModes;
	}

	private static int parameterIndex(String pattern, String index) {
		if ( index.isEmpty() ) {
			throw new IllegalArgumentException( "Missing parameter index in pattern: '" + pattern + "'" );
		}
		final int paramNumber;
		try {
			paramNumber = parseInt( index );
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException( "Illegal parameter index '" + index
												+ "' in pattern: '" + pattern + "'", nfe );
		}
		return paramNumber;
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
		render( sqlAppender, args, null, emptyList(), translator );
	}

	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			Predicate filter,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, args, filter, emptyList(), null, null, translator );
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
		render( sqlAppender, args, filter, emptyList(), respectNulls, fromFirst, translator );
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
		if ( numberOfArguments < maxParamIndex ) {
			CORE_LOGGER.missingArguments( maxParamIndex, numberOfArguments );
		}

		final boolean caseWrapper = filter != null && !filterClauseSupported( translator );
		for ( int i = 0; i < chunks.length; i++ ) {
			if ( i == varargParam ) {
				final var argumentRenderingMode = getArgumentRenderingMode(varargParam - 1);
				for ( int j = i; j < numberOfArguments; j++ ) {
					final SqlAstNode arg = args.get( j );
					if ( arg != null ) {
						sqlAppender.appendSql( chunks[i] );
						if ( caseWrapper
								&& !( arg instanceof Distinct )
								&& !( arg instanceof Star ) ) {
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
				final var arg = index < numberOfArguments ? args.get( index ) : null;
				if ( arg != null || i == 0 ) {
					sqlAppender.appendSql( chunks[i] );
				}
				if ( arg != null ) {
					if ( caseWrapper &&
							!( arg instanceof Distinct ) &&
							!( arg instanceof Star ) ) {
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
			sqlAppender.appendSql( fromFirst ? " from first" : " from last" );
		}
		if ( respectNulls != null ) {
			sqlAppender.appendSql( respectNulls ? " respect nulls" : " ignore nulls" );
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
		return index < argumentRenderingModes.length
				? argumentRenderingModes[index]
				: argumentRenderingModes[argumentRenderingModes.length - 1];
	}
}
