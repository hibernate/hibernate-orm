/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.OracleDialect}.
 *
 * @author Gavin King
 */
public final class OracleSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new OracleSequenceSupport();

	@Override
	public String getFromDual() {
		return " from dual";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
