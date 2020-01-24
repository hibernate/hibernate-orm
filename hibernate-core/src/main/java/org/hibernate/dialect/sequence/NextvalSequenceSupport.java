/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for dialects which support the common
 * Oracle-style syntax {@code seqname.nextval}.
 */
public class NextvalSequenceSupport implements SequenceSupport {

	@Override
	public final String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

}
