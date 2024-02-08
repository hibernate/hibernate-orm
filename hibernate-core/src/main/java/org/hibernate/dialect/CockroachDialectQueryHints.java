/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		List<IndexHint> indexHints = new ArrayList<>();
		JoinHint joinHint = null;
		for ( var h : hints ) {
			IndexHint indexHint = parseIndexHints( h );
			if ( indexHint != null ) {
				indexHints.add( indexHint );
				continue;
			}
			joinHint = parseJoinHints( h );
		}

		String result = addIndexHints( query, indexHints );
		return joinHint == null ? result : addJoinHint( query, joinHint );
	}

	private IndexHint parseIndexHints(String hint) {
		var parts = hint.split( "@" );
		if ( parts.length == 2 ) {
			return new IndexHint( parts[0], hint );
		}
		return null;
	}

	private JoinHint parseJoinHints(String hint) {
		var matcher = JOIN_HINT_PATTERN.matcher( hint );
		if ( matcher.find() ) {
			return new JoinHint( matcher.group( 1 ) );
		}
		return null;
	}

	String addIndexHints(String query, List<IndexHint> hints) {

		Matcher statementMatcher = TABLE_QUERY_PATTERN.matcher( query );

		if ( statementMatcher.matches() && statementMatcher.groupCount() > 2 ) {
			String prefix = statementMatcher.group( 1 );
			String fromList = statementMatcher.group( 2 );
			String suffix = statementMatcher.group( 3 );
			fromList = addIndexHintsToFromList( fromList, hints );
			return prefix + fromList + suffix;
		}
		else {
			return query;
		}
	}

	String addJoinHint(String query, JoinHint hint) {
		var m = JOIN_PATTERN.matcher( query );
		StringBuilder buffer = new StringBuilder();
		int start = 0;
		while ( m.find() ) {
			buffer.append(query, start, m.start());

			if ( m.group( 1 ) == null ) {
				buffer.append( " inner" );
			}
			else {
				buffer.append( m.group( 1 ) );
			}
			buffer.append( " " )
					.append( hint.joinType )
					.append( " join" );
			start = m.end();
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


	static class IndexHint {
		final String table;
		final String text;

		IndexHint(String table, String text) {
			this.table = table;
			this.text = text;
		}

	}

	static class JoinHint {
		final String joinType;

		JoinHint(String type) {
			this.joinType = type.toLowerCase( Locale.ROOT );
		}
	}
}
