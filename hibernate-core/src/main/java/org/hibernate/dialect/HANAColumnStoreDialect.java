/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.PhysicalIdTableExporter;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.type.StandardBasicTypes;

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
 * @author Andrew Clemons <andrew.clemons@sap.com>
 * @author Jonathan Bregler <jonathan.bregler@sap.com>
 */
public class HANAColumnStoreDialect extends AbstractHANADialect {

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// full-text search functions
		queryEngine.getSqmFunctionRegistry().registerNamed( "score", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "snippets" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "highlighted" );
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "contains", StandardBasicTypes.BOOLEAN, "contains(", ",", ") /*" );
		queryEngine.getSqmFunctionRegistry().registerPattern( "contains_rhs", "*/", StandardBasicTypes.BOOLEAN );
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "not_contains", StandardBasicTypes.BOOLEAN, "not_contains(", ",", ") /*" );
	}

	@Override
	public String getCreateTableString() {
		return "create column table";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableStrategy(
				new IdTable( entityDescriptor, basename -> "HT_" + basename ),
				() -> new PhysicalIdTableExporter() {
					@Override
					protected String getCreateCommand() {
						return "create global temporary column table";
					}

					@Override
					protected String getTruncateIdTableCommand() {
						return "truncate table";
					}
				},
				AfterUseAction.CLEAN,
				runtimeModelCreationContext.getSessionFactory()
		);
	}
}
