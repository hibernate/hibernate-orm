/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.regex.Pattern;

/**
 * A {@link LimitHandler} for Ingres.
 *
 * <p/>
 * From Ingres 10.2 Docs:
 * <pre>
 * Query
 * [ORDER BY clause]
 * [OFFSET n]
 * [FETCH {FIRST|NEXT} m ROWS ONLY]
 * [WITH clause]
 * </pre>
 *
 *  @author Gavin King
 */
public class IngresLimitHandler extends OffsetFetchLimitHandler {

	public static final IngresLimitHandler INSTANCE = new IngresLimitHandler();

	public IngresLimitHandler() {
		super(false);
	}

	@Override
	boolean isIngres() {
		//Ingres doesn't like "rows" in the
		//ANSI-standard syntax 'offset n rows'
		return true;
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
			Pattern.compile("\\s+with\\s+(" + String.join("|", WITH_OPTIONS) + ")\\b|\\s*(;|$)");

	@Override
	protected Pattern getForUpdatePattern() {
		return WITH_OPTION_PATTERN;
	}
}
