/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.community.dialect.MaxDBDialect;
import org.hibernate.dialect.sequence.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link MaxDBDialect}.
 *
 * @author Gavin King
 */
public final class MaxDBSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new MaxDBSequenceSupport();

	@Override
	public String getFromDual() {
		return " from dual";
	}

}
