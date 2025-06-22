/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.storedproc;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( value = PostgreSQLDialect.class )
@FailureExpected( jiraKey = "HHH-8445", reason = "Waiting on EG clarification" )
public class PostgresRefCursorSupportTest extends BaseSessionFactoryFunctionalTest {

	public static class ProcedureDefinitions implements AuxiliaryDatabaseObject, AuxiliaryDatabaseObject.Expandable {
		/**
		 * Singleton access
		 */
		public static final ProcedureDefinitions INSTANCE = new ProcedureDefinitions();

		@Override
		public void addDialectScope(String dialectName) {
			throw new IllegalStateException( "Not expecting addition of dialects to scope" );
		}

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return PostgreSQLDialect.class.isInstance( dialect );
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String getExportIdentifier() {
			return "function:all_items";
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			return new String[] {
					"create function all_items() return refcursor as \n" +
							"	'declare someCursor refcursor;\n" +
							"   begin\n" +
							"   	open someCursor for select * from ITEM;\n" +
							"       return someCursor;\n" +
							"   end;' language plpgsql;"
			};
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] {
					"drop function all_items()"
			};
		}
	}

	@Entity
	@Table( name = "PROC_ITEM" )
	public static class Item {
		@Id
		private Integer id;
		private String stockCode;
		private String name;
		private BigDecimal unitCost;
		@Temporal( TemporalType.TIMESTAMP )
		private Date availabilityStartDate;
		@Temporal( TemporalType.TIMESTAMP )
		private Date availabilityEndDate;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class
		};
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.applyMetadataBuilder( metadataBuilder );
		metadataBuilder.applyAuxiliaryDatabaseObject( ProcedureDefinitions.INSTANCE );
	}

	@Test
	public void testExplicitClassReturn() {
		inTransaction(
				session -> {
					ProcedureCall call = session.createStoredProcedureCall( "all_items", Item.class );
					call.registerParameter( 1, void.class, ParameterMode.REF_CURSOR );
					ProcedureOutputs outputs = call.getOutputs();
					ResultSetOutput results = assertTyping( ResultSetOutput.class, outputs.getCurrent() );
				}
		);
	}
}
