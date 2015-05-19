/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.storedproc;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ParameterMode;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( value = PostgreSQL81Dialect.class, strictMatching = false )
@FailureExpected( jiraKey = "HHH-8445", message = "Waiting on EG clarification" )
public class PostgresRefCursorSupportTest extends BaseUnitTestCase {

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
			return PostgreSQL81Dialect.class.isInstance( dialect )
					|| PostgreSQL82Dialect.class.isInstance( dialect );
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
		public String[] sqlCreateStrings(Dialect dialect) {
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
		public String[] sqlDropStrings(Dialect dialect) {
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

	private SessionFactory sf;

	@Before
	public void beforeTest() {
		Configuration cfg = new Configuration()
				.addAnnotatedClass( Item.class )
				.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		cfg.addAuxiliaryDatabaseObject( ProcedureDefinitions.INSTANCE );

		sf = cfg.buildSessionFactory();
	}

	@After
	public void afterTest() {
		if ( sf != null ) {
			sf.close();
		}
	}

	@Test
	public void testExplicitClassReturn() {
		Session session = sf.openSession();
		session.beginTransaction();

		ProcedureCall call = session.createStoredProcedureCall( "all_items", Item.class );
		call.registerParameter( 1, void.class, ParameterMode.REF_CURSOR );
		ProcedureOutputs outputs = call.getOutputs();
		ResultSetOutput results = assertTyping( ResultSetOutput.class, outputs.getCurrent() );

		session.getTransaction().commit();
		session.close();
	}
}
