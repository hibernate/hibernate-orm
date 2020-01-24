/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.MckoiDialect}.
 *
 * @author Gavin King
 */
public final class MckoiSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new MckoiSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval('" + sequenceName + "')";
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		return getCreateSequenceString( sequenceName )
				+ " increment " + incrementSize
				+ startingValue( initialValue, incrementSize )
				+ " start " + initialValue;
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
