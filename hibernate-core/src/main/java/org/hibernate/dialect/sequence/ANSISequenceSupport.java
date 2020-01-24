/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * ANSI SQL compliant sequence support, for dialects which
 * support the ANSI SQL syntax {@code next value for seqname}.
 *
 * @author Gavin King
 */
public class ANSISequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new ANSISequenceSupport();

	@Override
	public final String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}
}
