/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.FirebirdDialect}.
 *
 * @author Gavin King
 */
public final class FirebirdSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new FirebirdSequenceSupport();

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) {
		return new String[] {
				getCreateSequenceString( sequenceName ),
				"alter sequence " + sequenceName + " restart with " + initialValue
		};
	}

	@Override
	public String getFromDual() {
		return " from rdb$database";
	}

	@Override
	public String getSequenceNextValString(String sequenceName, int increment) {
		return increment == 1
				? getSequenceNextValString( sequenceName )
				: "select gen_id(" + sequenceName + "," + increment + ") from rdb$database";
	}
}
