/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;

/**
 * An SQL dialect for i. This class provides support for DB2 Universal Database for i V7R1 and
 * later, also known as DB2/400.
 *
 * @author Pierrick Rouxel (pierrickrouxel)
 */
public class DB2400V7R1Dialect extends DB2Dialect {

	private final UniqueDelegate uniqueDelegate;

	public DB2400V7R1Dialect() {
		super();

		uniqueDelegate = new DefaultUniqueDelegate(this);
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}

	@Override
	public String getQuerySequencesString() {
		return "select seqname from qsys2.syssequences";
	}
}
