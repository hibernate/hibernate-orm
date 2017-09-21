/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;

/**
 * An SQL dialect for HANA. <br/>
 * <a href="http://help.sap.com/hana/html/sqlmain.html">SAP HANA Reference</a> <br/>
 * Row tables are created by this dialect when using the auto-ddl feature.
 *
 * @author Andrew Clemons <andrew.clemons@sap.com>
 */
public class HANARowStoreDialect extends AbstractHANADialect {

	public HANARowStoreDialect() {
		super();
	}

	@Override
	public String getCreateTableString() {
		return "create row table";
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy( new IdTableSupportStandardImpl() {

			@Override
			public String getCreateIdTableCommand() {
				return "create global temporary row table";
			}
		}, AfterUseAction.CLEAN );
	}
}
