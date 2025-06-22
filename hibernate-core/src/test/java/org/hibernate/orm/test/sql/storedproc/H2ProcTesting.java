/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.storedproc;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

/**
 * @author Steve Ebersole
 */
public class H2ProcTesting {
	public static void applyProcDefinitions(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public String getExportIdentifier() {
						return "function:findOneUser";
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return dialect instanceof H2Dialect;
					}

					@Override
					public boolean beforeTablesOnCreation() {
						return false;
					}

					@Override
					public String[] sqlCreateStrings(SqlStringGenerationContext context) {
						return new String[] {
								"CREATE ALIAS findOneUser AS $$\n" +
										"import org.h2.tools.SimpleResultSet;\n" +
										"import java.sql.*;\n" +
										"@CODE\n" +
										"ResultSet findOneUser() {\n" +
										"    SimpleResultSet rs = new SimpleResultSet();\n" +
										"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
										"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
										"    rs.addRow(1, \"Steve\");\n" +
										"    return rs;\n" +
										"}\n" +
										"$$"
						};
					}

					@Override
					public String[] sqlDropStrings(SqlStringGenerationContext context) {
						return new String[] {
								"DROP ALIAS findUser IF EXISTS"
						};
					}
				}
		);

		metadataBuilder.applyAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public String getExportIdentifier() {
						return "function:findUsers";
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return dialect instanceof H2Dialect;
					}

					@Override
					public boolean beforeTablesOnCreation() {
						return false;
					}

					@Override
					public String[] sqlCreateStrings(SqlStringGenerationContext context) {
						return new String[] {
								"CREATE ALIAS findUsers AS $$\n" +
										"import org.h2.tools.SimpleResultSet;\n" +
										"import java.sql.*;\n" +
										"@CODE\n" +
										"ResultSet findUsers() {\n" +
										"    SimpleResultSet rs = new SimpleResultSet();\n" +
										"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
										"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
										"    rs.addRow(1, \"Steve\");\n" +
										"    rs.addRow(2, \"John\");\n" +
										"    rs.addRow(3, \"Jane\");\n" +
										"    return rs;\n" +
										"}\n" +
										"$$"
						};
					}

					@Override
					public String[] sqlDropStrings(SqlStringGenerationContext context) {
						return new String[] {"DROP ALIAS findUser IF EXISTS"};
					}
				}
		);

		metadataBuilder.applyAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public String getExportIdentifier() {
						return "function:findUserRange";
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return dialect instanceof H2Dialect;
					}

					@Override
					public boolean beforeTablesOnCreation() {
						return false;
					}

					@Override
					public String[] sqlCreateStrings(SqlStringGenerationContext context) {
						return new String[] {
								"CREATE ALIAS findUserRange AS $$\n" +
										"import org.h2.tools.SimpleResultSet;\n" +
										"import java.sql.*;\n" +
										"@CODE\n" +
										"ResultSet findUserRange(int start, int end) {\n" +
										"    SimpleResultSet rs = new SimpleResultSet();\n" +
										"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
										"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
										"    for ( int i = start; i < end; i++ ) {\n" +
										"        rs.addRow(1, \"User \" + i );\n" +
										"    }\n" +
										"    return rs;\n" +
										"}\n" +
										"$$"
						};
					}

					@Override
					public String[] sqlDropStrings(SqlStringGenerationContext context) {
						return new String[] {"DROP ALIAS findUserRange IF EXISTS"};
					}
				}
		);

	}

	@Entity
	@NamedStoredProcedureQueries( {
			@NamedStoredProcedureQuery(
					name = "findUserRangeNoNullPassing",
					procedureName = "findUserRange",
					parameters = {
							@StoredProcedureParameter( type = Integer.class ),
							@StoredProcedureParameter( type = Integer.class ),
					}
			),
			@NamedStoredProcedureQuery(
					name = "findUserRangeNamedNullPassing",
					procedureName = "findUserRange",
					hints = @QueryHint( name = "hibernate.proc.param_null_passing.firstArg", value = "true" ),
					parameters = {
							@StoredProcedureParameter( name = "firstArg", type = Integer.class ),
							@StoredProcedureParameter( name = "secondArg", type = Integer.class ),
					}
			),
			@NamedStoredProcedureQuery(
					name = "findUserRangeOrdinalNullPassing",
					procedureName = "findUserRange",
					hints = @QueryHint( name = "hibernate.proc.param_null_passing.1", value = "true" ),
					parameters = {
							@StoredProcedureParameter( type = Integer.class ),
							@StoredProcedureParameter( type = Integer.class ),
					}
			)
	} )
	@SqlResultSetMapping(
			name = "all-fields",
			entities = @EntityResult(
					entityClass = MyEntity.class,
					fields = {
							@FieldResult( name = "id", column = "id" ),
							@FieldResult( name = "name", column = "name" )
					}
			)
	)
	@SqlResultSetMapping(
			name = "some-fields",
			entities = @EntityResult(
					entityClass = MyEntity.class,
					fields = {
							@FieldResult( name = "id", column = "id" )
					}
			)
	)
	@SqlResultSetMapping(
			name = "no-fields",
			entities = @EntityResult(
					entityClass = MyEntity.class
			)
	)
	public static class MyEntity {
		@Id
		public Integer id;
		String name;
	}
}
