/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for DB2 11.1 which supports the
 * ANSI SQL standard syntax {@code FETCH FIRST m ROWS ONLY}
 * and {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY},
 * with the only wrinkle being that this clause comes
 * after the {@code FOR UPDATE} and other similar clauses.
 *
 * @author Gavin King
 */
public class DB2LimitHandler extends OffsetFetchLimitHandler {

	public static final DB2LimitHandler INSTANCE = new DB2LimitHandler();

	public DB2LimitHandler() {
		super(true);
	}

	@Override
	String insert(String offsetFetch, String sql) {
		//on DB2, offset/fetch comes after all the
		//various "for update"ish clauses
		//see https://www.ibm.com/support/knowledgecenter/SSEPGG_11.1.0/com.ibm.db2.luw.sql.ref.doc/doc/r0000879.html
		return super.insertAtEnd( offsetFetch, sql );
	}
}
