/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.InterbaseDialect}.
 *
 * @author Gavin King
 */
public final class InterbaseSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new InterbaseSequenceSupport();

	@Override
	public String getSequenceNextValString(String sequenceName, int increment) {
		return "select " + getSelectSequenceNextValString( sequenceName, increment ) + getFromDual();
	}

	@Override
	public String getFromDual() {
		return " from rdb$database";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return getSelectSequenceNextValString( sequenceName, 1 );
	}

	public String getSelectSequenceNextValString(String sequenceName, int increment) {
		return "gen_id(" + sequenceName + "," + increment + ")";
	}

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) {
		return initialValue == 1
				? new String[] { getCreateSequenceString(sequenceName) }
				: new String[] {
						getCreateSequenceString(sequenceName),
						"set generator " + sequenceName
								+ " to " + (initialValue - incrementSize)
				};
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create generator " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop generator " + sequenceName;
	}

}
