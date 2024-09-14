/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.AltibaseDialect}.
 *
 * @author Geoffrey park
 */
public class AltibaseSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new AltibaseSequenceSupport();

	@Override
	public String getFromDual() {
		return " from dual";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
