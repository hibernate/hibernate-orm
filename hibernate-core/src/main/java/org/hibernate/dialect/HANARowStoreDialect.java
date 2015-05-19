/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for HANA. <br/>
 * <a href="http://help.sap.com/hana/html/sqlmain.html">SAP HANA Reference</a> <br/>
 * Row tables are created by this dialect when using the auto-ddl feature.
 *
 * @author Andrew Clemons <andrew.clemons@sap.com>
 */
public class HANARowStoreDialect extends AbstractHANADialect {

	// Even though it's currently pointless, provide this structure in case HANA row store merits additional
	// differences in the future.

	public HANARowStoreDialect() {
		super();
	}
}
