/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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

	private static final Pattern QUERY_PATTERN = Pattern.compile( "^\\s*(select\\b.+?\\bfrom\\b.+?)(\\bwhere\\b.+?)$" );

	@Override
	public String addQueryHints(String query, String hints) {
		Matcher matcher = QUERY_PATTERN.matcher( query );
		if ( matcher.matches() && matcher.groupCount() > 1 ) {
			String startToken = matcher.group( 1 );
			String endToken = matcher.group( 2 );

			return startToken +
					" use index (" +
					hints +
					") " +
					endToken;
		}
		else {
			return query;
		}
	}
}
