/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for Transact SQL and similar
 * databases which support the syntax {@code SELECT TOP n}.
 * Note that this syntax does not allow specification of
 * an offset.
 *
 * @author Brett Meyer
 */
public class TopLimitHandler extends AbstractNoOffsetLimitHandler {

	public static TopLimitHandler INSTANCE = new TopLimitHandler(true);

	public TopLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " top ? ";
	}

	@Override
	protected String insert(String limitClause, String sql) {
		return insertAfterDistinct( limitClause, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

}
