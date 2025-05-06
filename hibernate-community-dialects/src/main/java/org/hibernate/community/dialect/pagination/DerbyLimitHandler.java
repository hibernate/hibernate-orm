/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import java.util.regex.Pattern;

import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;

/**
 * A {@link LimitHandler} for Apache Derby, which
 * fully supports the ANSI SQL standard syntax
 * {@code FETCH FIRST m ROWS ONLY} and
 * {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 */
public class DerbyLimitHandler extends OffsetFetchLimitHandler {

	// [ORDER BY ...]
	// [OFFSET n {ROW|ROWS}]
	// [FETCH {FIRST|NEXT} m {ROW|ROWS} ONLY]
	// [FOR {UPDATE|READ ONLY|FETCH ONLY}]
	// [WITH {RR|RS|CS|UR}]

	public DerbyLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	private static final Pattern FOR_UPDATE_WITH_LOCK_PATTERN =
			Pattern.compile("\\s+for\\s+(update|read|fetch)\\b|\\s+with\\s+(rr|rs|cs|ur)\\b|\\s*;?\\s*$");

	/**
	 * The offset/fetch clauses must come before the
	 * {@code FOR UPDATE}ish and {@code WITH} clauses.
	 */
	@Override
	protected Pattern getForUpdatePattern() {
		//see https://db.apache.org/derby/docs/10.15/ref/rrefsqljoffsetfetch.html#rrefsqljoffsetfetch
		return FOR_UPDATE_WITH_LOCK_PATTERN;
	}
}
