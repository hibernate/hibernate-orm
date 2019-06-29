/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.CUBRIDDialect}.
 *
 * @author Gavin King
 */
public final class CUBRIDSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new CUBRIDSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".next_value";
	}

	@Override
	public String getFromDual() {
		//TODO: is this really needed?
		//TODO: would " from db_root" be better?
		return " from table({1}) as T(X)";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create serial " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop serial " + sequenceName;
	}
}
