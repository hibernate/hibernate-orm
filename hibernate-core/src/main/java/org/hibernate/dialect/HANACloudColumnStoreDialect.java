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
 */
public class HANACloudColumnStoreDialect extends AbstractHANADialect {

	public HANACloudColumnStoreDialect() {
		super();

		registerColumnType( Types.CHAR, "nvarchar(1)" );
		registerColumnType( Types.VARCHAR, 5000, "nvarchar($l)" );
		registerColumnType( Types.LONGVARCHAR, 5000, "nvarchar($l)" );

		// for longer values map to clob/nclob
		registerColumnType( Types.LONGVARCHAR, "nclob" );
		registerColumnType( Types.VARCHAR, "nclob" );
		registerColumnType( Types.CLOB, "nclob" );

		registerHibernateType( Types.CLOB, StandardBasicTypes.MATERIALIZED_NCLOB.getName() );
		registerHibernateType( Types.NCHAR, StandardBasicTypes.NSTRING.getName() );
		registerHibernateType( Types.CHAR, StandardBasicTypes.CHARACTER.getName() );
		registerHibernateType( Types.CHAR, 1, StandardBasicTypes.CHARACTER.getName() );
		registerHibernateType( Types.CHAR, 5000, StandardBasicTypes.NSTRING.getName() );
		registerHibernateType( Types.VARCHAR, StandardBasicTypes.NSTRING.getName() );
		registerHibernateType( Types.LONGVARCHAR, StandardBasicTypes.NTEXT.getName() );

		// register additional keywords
		registerHanaCloudKeywords();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// full-text search functions
		queryEngine.getSqmFunctionRegistry().registerNamed( "score", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "snippets" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "highlighted" );
//		queryEngine.getSqmFunctionRegistry().registerVarArgs( "contains", StandardSpiBasicTypes.BOOLEAN, "contains(", ",", ") /*" );
//		queryEngine.getSqmFunctionRegistry().registerPattern( "contains_rhs", "*/", StandardSpiBasicTypes.BOOLEAN );
//		queryEngine.getSqmFunctionRegistry().registerVarArgs( "not_contains", StandardSpiBasicTypes.BOOLEAN, "not_contains(", ",", ") /*" );
	}

	private void registerHanaCloudKeywords() {
		registerKeyword( "array" );
		registerKeyword( "at" );
		registerKeyword( "authorization" );
		registerKeyword( "between" );
		registerKeyword( "by" );
		registerKeyword( "collate" );
		registerKeyword( "empty" );
		registerKeyword( "filter" );
		registerKeyword( "grouping" );
		registerKeyword( "no" );
		registerKeyword( "not" );
		registerKeyword( "of" );
		registerKeyword( "over" );
		registerKeyword( "recursive" );
		registerKeyword( "row" );
		registerKeyword( "table" );
		registerKeyword( "to" );
		registerKeyword( "window" );
		registerKeyword( "within" );
	}

	@Override
	public String getCreateTableString() {
		return "create column table";
	}


	@Override
	protected boolean supportsAsciiStringTypes() {
		return false;
	}

	@Override
	protected Boolean useUnicodeStringTypesDefault() {
		return Boolean.TRUE;
	}

	@Override
	public boolean isUseUnicodeStringTypes() {
		return true;
	}
}
