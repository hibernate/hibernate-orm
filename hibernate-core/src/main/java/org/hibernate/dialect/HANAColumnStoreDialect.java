/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;

/**
 * An SQL dialect for the SAP HANA column store.
 * <p>
 * For more information on interacting with the SAP HANA database, refer to the
 * <a href="https://help.sap.com/viewer/4fe29514fd584807ac9f2a04f6754767/">SAP HANA SQL and System Views Reference</a>
 * and the <a href=
 * "https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/latest/en-US/434e2962074540e18c802fd478de86d6.html">SAP
 * HANA Client Interface Programming Reference</a>.
 * <p>
 * Column tables are created by this dialect when using the auto-ddl feature.
 * 
 * @author Andrew Clemons
 * @author Jonathan Bregler
 *
 * @deprecated use {@link HANADialect} instead
 */
@Deprecated(forRemoval = true)
public class HANAColumnStoreDialect extends AbstractHANADialect {

	public HANAColumnStoreDialect(DialectResolutionInfo info) {
		this( HANAServerConfiguration.fromDialectResolutionInfo( info ) );
		registerKeywords( info );
	}
	
	public HANAColumnStoreDialect() {
		// SAP HANA 1.0 SP12 is the default
		this( DatabaseVersion.make( 1, 0, 120 ) );
	}

	public HANAColumnStoreDialect(DatabaseVersion version) {
		this( new HANAServerConfiguration( version ) );
	}

	public HANAColumnStoreDialect(HANAServerConfiguration configuration) {
		super( configuration, true );
	}
}
