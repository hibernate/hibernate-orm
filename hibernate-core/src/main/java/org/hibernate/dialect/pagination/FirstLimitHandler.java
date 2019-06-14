/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for older versions of Informix, Ingres,
 * and TimesTen, which supported the syntax {@code SELECT FIRST n}.
 * Note that this syntax does not allow specification of an offset.
 *
 * @author Chris Cranford
 */
public class FirstLimitHandler extends AbstractNoOffsetLimitHandler {

	public static final FirstLimitHandler INSTANCE = new FirstLimitHandler(false);

	public FirstLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " first ?";
	}

	@Override
	protected String insert(String first, String sql) {
		return insertAfterSelect( first, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

}
