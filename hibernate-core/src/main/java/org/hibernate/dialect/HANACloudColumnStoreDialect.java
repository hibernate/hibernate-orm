/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for the SAP HANA Cloud column store.
 * <p>
 * For more information on interacting with the SAP HANA Cloud database, refer to the
 * <a href="https://help.sap.com/viewer/c1d3f60099654ecfb3fe36ac93c121bb/cloud/">SAP HANA Cloud SQL Reference Guide</a>
 * and the <a href=
 * "https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/latest/en-US/434e2962074540e18c802fd478de86d6.html">SAP
 * HANA Client Interface Programming Reference</a>.
 * <p>
 * Column tables are created by this dialect when using the auto-ddl feature.
 * 
 * @author <a href="mailto:jonathan.bregler@sap.com">Jonathan Bregler</a>
 *
 * @deprecated use HANAColumnStoreDialect(400)
 */
@Deprecated
public class HANACloudColumnStoreDialect extends HANAColumnStoreDialect {

	public HANACloudColumnStoreDialect() {
		super( DatabaseVersion.make( 4 ) );
	}

}
