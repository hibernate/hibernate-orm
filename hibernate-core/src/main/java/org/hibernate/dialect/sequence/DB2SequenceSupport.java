/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2Dialect}.
 *
 * @author Gavin King
 */
public class DB2SequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new DB2SequenceSupport();

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "previous value for " + sequenceName;
	}

	@Override
	public String getSequencePreviousValString(String sequenceName) {
		return "values " + getSelectSequencePreviousValString( sequenceName );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
