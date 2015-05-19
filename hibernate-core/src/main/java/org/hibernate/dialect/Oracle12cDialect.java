/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQL2008StandardLimitHandler;

/**
 * An SQL dialect for Oracle 12c.
 *
 * @author zhouyanming (zhouyanming@gmail.com)
 */
public class Oracle12cDialect extends Oracle10gDialect {
	public Oracle12cDialect() {
		super();
	}

	@Override
	protected void registerDefaultProperties() {
		super.registerDefaultProperties();
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String getIdentityColumnString() {
		return "generated as identity";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return SQL2008StandardLimitHandler.INSTANCE;
	}

}
