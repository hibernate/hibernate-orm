/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identity.DB2390IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * An SQL dialect for DB2/400.  This class provides support for
 * DB2 Universal Database for iSeries, also known as DB2/400.
 *
 * @author Peter DeGregorio (pdegregorio)
 */
public class DB2400Dialect extends DB2Dialect {

	/**
	 * No support for sequences.
	 */
	@Override
	public SequenceSupport getSequenceSupport() {
		return NoSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return null;
	}

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return FetchLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2390IdentityColumnSupport();
	}
}
