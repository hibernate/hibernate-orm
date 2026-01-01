/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.hibernate.internal.util.StringHelper.split;

class CockroachDialectQueryHints {

	final private Pattern TABLE_QUERY_PATTERN = Pattern.compile(
			"(?i)^\\s*(select\\b.+?\\bfrom\\b)(.+?)(\\bwhere\\b.+?)$" );
	final private Pattern JOIN_HINT_PATTERN = Pattern.compile( "(?i)(MERGE|HASH|LOOKUP|INVERTED)\\s+JOIN" );

	//If matched, group 1 contains everything before the join keyword.
	final private Pattern JOIN_PATTERN = Pattern.compile(
			"(?i)\\b(cross|natural\\s+(.*)\\b|(full|left|right)(\\s+outer)?)?\\s+join" );

	final private String query;
	final private List<String> hints;

	public CockroachDialectQueryHints(String query, List<String> hintList) {
		this.query = query;
		this.hints = hintList;
	}

	public String getQueryHintString() {
		final List<IndexHint> indexHints = new ArrayList<>();
		JoinHint joinHint = null;
		for ( var hint : hints ) {
			final var indexHint = parseIndexHints( hint );
			if ( indexHint != null ) {
				indexHints.add( indexHint );
			}
			else {
				joinHint = parseJoinHints( hint );
			}
		}

		final String result = addIndexHints( query, indexHints );
		return joinHint == null ? result : addJoinHint( query, joinHint );
	}

	private IndexHint parseIndexHints(String hint) {
		final var parts = split( "@", hint );
		return parts.length == 2 ? new IndexHint( parts[0], hint ) : null;
	}

	private JoinHint parseJoinHints(String hint) {
		var matcher = JOIN_HINT_PATTERN.matcher( hint );
		return matcher.find() ? new JoinHint( matcher.group( 1 ) ) : null;
	}

	private String addIndexHints(String query, List<IndexHint> hints) {
		final var statementMatcher = TABLE_QUERY_PATTERN.matcher( query );
		if ( statementMatcher.matches() && statementMatcher.groupCount() > 2 ) {
			final String prefix = statementMatcher.group( 1 );
			final String fromList = addIndexHintsToFromList( statementMatcher.group( 2 ), hints );
			final String suffix = statementMatcher.group( 3 );
			return prefix + fromList + suffix;
		}
		else {
			return query;
		}
	}

	private String addJoinHint(String query, JoinHint hint) {
		final var matcher = JOIN_PATTERN.matcher( query );
		final var buffer = new StringBuilder();
		int start = 0;
		while ( matcher.find() ) {
			buffer.append( query, start, matcher.start() );
			if ( matcher.group( 1 ) == null ) {
				buffer.append( " inner" );
			}
			else {
				buffer.append( matcher.group( 1 ) );
			}
			buffer.append( " " )
					.append( hint.joinType )
					.append( " join" );
			start = matcher.end();
		}
		buffer.append( query.substring( start ) );
		return buffer.toString();
	}

	String addIndexHintsToFromList(String fromList, List<IndexHint> hints) {
		String result = fromList;
		for ( var hint : hints ) {
			result = result.replaceAll( "(?i)\\b" + hint.table + "\\b", hint.text );
		}
		return result;
	}


	private record IndexHint(String table, String text) {
	}

	private record JoinHint(String joinType) {
		JoinHint(String joinType) {
			this.joinType = joinType.toLowerCase( Locale.ROOT );
		}
	}
}
