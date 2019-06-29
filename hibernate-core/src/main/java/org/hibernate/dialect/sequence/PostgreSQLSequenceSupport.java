/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.PostgreSQLDialect}.
 *
 * @author Gavin King
 */
public class PostgreSQLSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new PostgreSQLSequenceSupport();

	public static final SequenceSupport LEGACY_INSTANCE = new PostgreSQLSequenceSupport() {
		@Override
		public String getDropSequenceString(String sequenceName) {
			return "drop sequence " + sequenceName;
		}
	};

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval('" + sequenceName + "')";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

}
