/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.H2Dialect}.
 *
 * @author Gavin King
 */
public final class H2SequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new H2SequenceSupport();

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call " + getSelectSequenceNextValString( sequenceName );
	}
}
