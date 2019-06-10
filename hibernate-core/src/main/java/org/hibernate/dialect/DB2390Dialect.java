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
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;


/**
 * An SQL dialect for DB2/390. This class provides support for
 * DB2 Universal Database for OS/390, also known as DB2/390.
 *
 * @author Kristoffer Dyrkorn
 */
public class DB2390Dialect extends DB2Dialect {

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
	public boolean supportsSequences() {
		return get390Version() >= 8;
	}

	@Override
	public String getQuerySequencesString() {
		return get390Version() < 8 ? null : "select * from sysibm.syssequences";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select nextval for " + sequenceName + " from sysibm.sysdummy1";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName + " as integer start with 1 increment by 1 minvalue 1 nomaxvalue nocycle nocache"; //simple default settings..
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
