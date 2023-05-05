/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Microsoft SQL Server 2016 Dialect
 * @deprecated use {@code SQLServerDialect(13)}
 */
@Deprecated
public class SQLServer2016Dialect extends SQLServerDialect {

	public SQLServer2016Dialect() {
		super( DatabaseVersion.make( 13 ) );
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema if exists " + schemaName};
	}
}
