/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.QueryParameter;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.sql.storedproc.StoredProcedureResultSetMappingTest;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureApiTests extends BaseNonConfigCoreFunctionalTestCase {


	@Test
	public void parameterValueAccess() {
		inTransaction(
				session -> {
					final ProcedureCall call = session.createStoredProcedureCall( "test" );

					call.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
					call.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT);
					call.setParameter( 1, 1 );
					call.getParameterValue( 1 );
				}
		);
	}

	@Test
	public void testInvalidParameterReference() {
		inTransaction(
				session -> {
					final ProcedureCall call1 = session.createStoredProcedureCall( "test" );
					call1.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
					final Parameter<Integer> p1_1 = (Parameter<Integer>) call1.getParameter( 1 );
					call1.setParameter( 1, 1 );

					final ProcedureCall call2 = session.createStoredProcedureCall( "test" );
					call2.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
					call2.setParameter( 1, 1 );

					try {
						call2.getParameterValue( p1_1 );
						fail( "Expecting failure" );
					}
					catch (IllegalArgumentException expected) {

					}
				}
		);
	}

	@Test
	public void testParameterBindTypeMismatch() {
		inTransaction(
				session -> {
					try {
						final ProcedureCall call1 = session.createStoredProcedureCall( "test" );
						call1.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
						call1.setParameter( 1, new Date() );

						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
					}
				}
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );

		sources.addAnnotatedClass( Person.class );
	}

	@Override
	protected void afterMetadataBuilt(Metadata metadata) {
		super.afterMetadataBuilt( metadata );

//		metadata.getDatabase().addAuxiliaryDatabaseObject(
//				new StoredProcedureResultSetMappingTest.ProcedureDefinition() {
//					@Override
//					public boolean appliesToDialect(Dialect dialect) {
//						return H2Dialect.class.isInstance( dialect );
//					}
//
//					@Override
//					public boolean beforeTablesOnCreation() {
//						return false;
//					}
//
//					@Override
//					public String getExportIdentifier() {
//						return "StoredProcedure#test"
//					}
//
//					@Override
//					public String[] sqlCreateStrings(Dialect dialect) {
//						return super.sqlCreateStrings( dialect );
//					}
//
//					@Override
//					public String[] sqlDropStrings(Dialect dialect) {
//						return super.sqlDropStrings( dialect );
//					}
//				}
//		);
	}

	@Entity( name = "Person" )
	@Table( name = "person" )
	public static class Person {
		@Id
		public Integer id;
		public String name;
	}
}
