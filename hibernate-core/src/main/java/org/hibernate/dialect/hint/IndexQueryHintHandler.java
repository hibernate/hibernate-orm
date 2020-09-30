/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.hint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds an INDEX query hint as follows:
 *
 * <code>
 * SELECT *
 * FROM TEST
 * USE INDEX (hint1, hint2)
 * WHERE X=1
 * </code>
 *
 * @author Vlad Mihalcea
 */
public class IndexQueryHintHandler implements QueryHintHandler {

	public static final IndexQueryHintHandler INSTANCE = new IndexQueryHintHandler();

	private static final Pattern QUERY_PATTERN = Pattern.compile( "^(select.*?from.*?)(where.*?)$" );

	@Override
	public String addQueryHints(String query, String hints) {
		Matcher matcher = QUERY_PATTERN.matcher( query );
		if ( matcher.matches() && matcher.groupCount() > 1 ) {
			String startToken = matcher.group( 1 );
			String endToken = matcher.group( 2 );

			return new StringBuilder( startToken )
					.append( " USE INDEX (" )
					.append( hints )
					.append( ") " )
					.append( endToken )
					.toString();
		}
		else {
			return query;
		}
	}
}
