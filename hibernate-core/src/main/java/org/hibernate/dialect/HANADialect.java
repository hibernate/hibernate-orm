/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * An SQL dialect for the SAP HANA Platform and Cloud.
 * <p>
 * For more information on SAP HANA Cloud, refer to the
 * <a href="https://help.sap.com/docs/hana-cloud-database/sap-hana-cloud-sap-hana-database-sql-reference-guide/sap-hana-cloud-sap-hana-database-sql-reference-guide">SAP HANA Cloud SQL Reference Guide</a>.
 * For more information on SAP HANA Platform, refer to the
 * <a href="https://help.sap.com/docs/SAP_HANA_PLATFORM/4fe29514fd584807ac9f2a04f6754767/b4b0eec1968f41a099c828a4a6c8ca0f.html?locale=en-US">SAP HANA Platform SQL Reference Guide</a>.
 * <p>
 * Column tables are created by this dialect by default when using the auto-ddl feature.
 *
 * @author Andrew Clemons
 * @author Jonathan Bregler
 */
@SuppressWarnings("removal")
public class HANADialect extends AbstractHANADialect {

	static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 1, 0, 120 );

	public HANADialect(DialectResolutionInfo info) {
		this( HANAServerConfiguration.fromDialectResolutionInfo( info ), true );
		registerKeywords( info );
	}

	public HANADialect() {
		// SAP HANA 1.0 SPS12 R0 is the default
		this( MINIMUM_VERSION );
	}

	public HANADialect(DatabaseVersion version) {
		this( new HANAServerConfiguration( version ), true );
	}

	public HANADialect(DatabaseVersion version, boolean defaultTableTypeColumn) {
		this( new HANAServerConfiguration( version ), defaultTableTypeColumn );
	}

	public HANADialect(HANAServerConfiguration configuration, boolean defaultTableTypeColumn) {
		super( configuration, defaultTableTypeColumn );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}
}
