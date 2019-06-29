/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.MimerSQLDialect}.
 *
 * @author Gavin King
 */
public class MimerSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new MimerSequenceSupport();

	@Override
	public String getFromDual() {
		return " from system.onerow";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
