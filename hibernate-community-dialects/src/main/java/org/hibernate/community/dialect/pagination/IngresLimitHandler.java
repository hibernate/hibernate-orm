/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.pagination;

import java.util.regex.Pattern;

import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;

/**
 * A {@link LimitHandler} for Ingres. According to the
 * documentation for Ingres 10.2, Ingres supports the
 * syntax {@code FETCH FIRST m ROWS ONLY}
 * and {@code OFFSET n FETCH NEXT m ROWS ONLY}, which
 * is oh-so-close to the ANSI SQL standard, except for
 * the missing {@code ROWS} after {@code OFFSET}.
 *
 *  @author Gavin King
 */
public class IngresLimitHandler extends OffsetFetchLimitHandler {

	// [ORDER BY ...]
	// [OFFSET n]
	// [FETCH {FIRST|NEXT} m {ROW|ROWS} ONLY]
	// [WITH options]

	public static final IngresLimitHandler INSTANCE = new IngresLimitHandler();

	public IngresLimitHandler() {
		super(false);
	}

	@Override
	protected boolean renderOffsetRowsKeyword() {
		//Ingres doesn't like "rows" in the
		//ANSI-standard syntax 'offset n rows'
		return false;
	}

	private static final String[] WITH_OPTIONS = {
			"keyj",
			"flatten",
			"ojflatten",
			"qep",
			"greedy",
			"hash",
			"hashagg",
			"hashjoin",
			"parallel",
			"union_flatten",
			"nokeyj",
			"noflatten",
			"noojflatten",
			"noqep",
			"nogreedy",
			"nohash",
			"nohashagg",
			"nohashjoin",
			"noparallel",
			"nounion_flatten",
			"max_parallel"
	};

	private static final Pattern WITH_OPTION_PATTERN =
			Pattern.compile("\\s+with\\s+(" + String.join("|", WITH_OPTIONS) + ")\\b|\\s*;?\\s*$");

	/**
	 * The offset/fetch clauses must come before
	 * the {@code WITH} clause.
	 */
	@Override
	protected Pattern getForUpdatePattern() {
		//see https://docs.actian.com/ingres/10.2/index.html#page/SQLRef%2FSelect_(interactive)_Syntax.htm%23
		return WITH_OPTION_PATTERN;
	}
}
