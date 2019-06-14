/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for databases which support the ANSI
 * SQL standard syntax {@code FETCH FIRST m ROWS ONLY} but not
 * {@code OFFSET n ROWS}.
 *
 * @author Gavin King
 */
public class FetchLimitHandler extends AbstractNoOffsetLimitHandler {

	public static final FetchLimitHandler INSTANCE = new FetchLimitHandler(false);

	public FetchLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " fetch first ? rows only";
	}

	@Override
	protected String insert(String fetch, String sql) {
		return insertBeforeForUpdate( fetch, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

}
