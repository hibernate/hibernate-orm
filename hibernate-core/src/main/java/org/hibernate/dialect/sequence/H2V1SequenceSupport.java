/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.H2Dialect}.
 *
 * @author Gavin King
 */
public final class H2V1SequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new H2V1SequenceSupport();

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return sequenceName + ".currval";
	}
}
