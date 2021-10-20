/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.PhysicalIdTableExporter;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/**
 * An SQL dialect for the SAP HANA row store.
 * <p>
 * For more information on interacting with the SAP HANA database, refer to the
 * <a href="https://help.sap.com/viewer/4fe29514fd584807ac9f2a04f6754767/">SAP HANA SQL and System Views Reference</a>
 * and the <a href=
 * "https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/latest/en-US/434e2962074540e18c802fd478de86d6.html">SAP
 * HANA Client Interface Programming Reference</a>.
 * <p>
 * Row tables are created by this dialect when using the auto-ddl feature.
 *
 * @author <a href="mailto:andrew.clemons@sap.com">Andrew Clemons</a>
 * @author <a href="mailto:jonathan.bregler@sap.com">Jonathan Bregler</a>
 */
public class HANARowStoreDialect extends AbstractHANADialect {

	public HANARowStoreDialect() {
		super();
	}

	public HANARowStoreDialect(DialectResolutionInfo info) {
		super();
		registerKeywords( info );
	}

	@Override
	public String getCreateTableString() {
		return "create row table";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableStrategy(
				new IdTable( entityDescriptor, basename -> "HT_" + basename, this ),
				() -> new PhysicalIdTableExporter() {
					@Override
					protected String getCreateCommand() {
						return "create global temporary row table";
					}

					@Override
					public String getCreateOptions() {
						return "on commit delete rows";
					}
				},
				AfterUseAction.CLEAN,
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public int getVersion() {
		return 0;
	}

	@Override
	protected boolean supportsAsciiStringTypes() {
		return true;
	}

	@Override
	protected Boolean useUnicodeStringTypesDefault() {
		return Boolean.FALSE;
	}
}
