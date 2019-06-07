/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Delegate for handling function "templates".
 *
 * @author Steve Ebersole
 */
public class PatternRenderer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PatternRenderer.class );

	private final String[] chunks;
	private final int[] paramIndexes;
	private final int paramCount;

	/**
	 * Constructs a template renderer
	 *
	 * @param pattern The template
	 */
	public PatternRenderer(String pattern) {
		final Set<Integer> paramNumbers = new HashSet<>();
		final List<String> chunkList = new ArrayList<>();
		final List<Integer> paramList = new ArrayList<>();
		final StringBuilder chunk = new StringBuilder( 10 );
		final StringBuilder index = new StringBuilder( 2 );

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
					else if ( c  == '?' ) {
						i--;
						break;
					}
					else {
						chunk.append( c );
						break;
					}
				}

				Integer paramNumber = Integer.valueOf( index.toString() );
				paramNumbers.add( paramNumber );
				paramList.add( paramNumber );
				index.setLength(0);
			}
			else {
				chunk.append( c );
			}
			i++;
		}

		if ( chunk.length() > 0 ) {
			chunkList.add( chunk.toString() );
		}

		chunks = chunkList.toArray( new String[chunkList.size()] );
		paramIndexes = new int[paramList.size()];
		paramCount = paramNumbers.size();
		for ( i = 0; i < paramIndexes.length; ++i ) {
			paramIndexes[i] = paramList.get( i );
		}
	}

//	public String getPattern() {
//		return pattern;
//	}

	public int getParamCount() {
		return paramCount;
	}

	/**
	 * The rendering code.
	 *
	 * @param args The arguments to inject into the template
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> args,
			SqlAstWalker walker) {
		final int numberOfArguments = args.size();
		if ( numberOfArguments != paramCount ) {
			LOG.missingArguments( paramCount, numberOfArguments );
		}

		for ( int i = 0; i < chunks.length; ++i ) {
			if ( i < paramIndexes.length ) {
				final int index = paramIndexes[i] - 1;
				final SqlAstNode arg = index < numberOfArguments ? args.get( index ) : null;
				if ( arg != null ) {
					sqlAppender.appendSql( chunks[i] );
					arg.accept( walker );
				}
			}
			else {
				sqlAppender.appendSql( chunks[i] );
			}
		}
	}
}
