/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identity.DB2zIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.FetchLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DB2zSequenceSupport;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;


/**
 * An SQL dialect for DB2/390. This class provides support for
 * DB2 Universal Database for OS/390, also known as DB2/390.
 *
 * @author Kristoffer Dyrkorn
 * @deprecated Use {@link DB2LegacyDialect}
 */
@Deprecated
public class DB2390Dialect extends DB2LegacyDialect {

	private final int version;

	int get390Version() {
		return version;
	}

	public DB2390Dialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() );
	}

	public DB2390Dialect() {
		this(7);
	}

	public DB2390Dialect(int version) {
		super();
		this.version = version;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return get390Version() < 8
				? NoSequenceSupport.INSTANCE
				: DB2zSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return get390Version() < 8 ? null : "select * from sysibm.syssequences";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return FetchLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return DB2zIdentityColumnSupport.INSTANCE;
	}
}
