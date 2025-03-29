/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import java.sql.PreparedStatement;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@RequiresDialect(HANADialect.class)
public class HANACalcViewTest extends BaseCoreFunctionalTestCase {

	private static final String CALC_VIEW_NAME = "DUMMY_CV_VIEW";
	private static final String CALC_SCENARIO_NAME = "DUMMY_CV_SCEN";
	private static final String PROJECTION_NODE_NAME = "DUMMY_PROJ";

	@Override
	protected void afterSessionFactoryBuilt() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection
						.prepareStatement( "CREATE CALCULATION SCENARIO " + CALC_SCENARIO_NAME + " USING '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
								"<cubeSchema version=\"2\" operation=\"createCalculationScenario\">\r\n" +
								"<calculationScenario>\r\n" +
								"  <dataSources>\r\n" +
								"    <tableDataSource name=\"DUMMY_DS\" schema=\"SYS\" table=\"DUMMY\">\r\n" +
								"      <attributes>\r\n" +
								"        <allAttribute/>\r\n" +
								"      </attributes>\r\n" +
								"    </tableDataSource>\r\n" +
								"  </dataSources>\r\n" +
								"  <calculationViews>\r\n" +
								"    <projection name=\"" + PROJECTION_NODE_NAME + "\" defaultViewFlag=\"true\">\r\n" +
								"      <inputs>\r\n" +
								"        <input name=\"DUMMY_DS\" />\r\n" +
								"      </inputs>\r\n" +
								"      <attributes>\r\n" +
								"        <allAttribute />\r\n" +
								"        <calculatedAttribute name=\"DUMMYDUMMY\" datatype=\"string\" isViewAttribute=\"true\">\r\n" +
								"          <formula>\"DUMMY\" + \"DUMMY\"</formula>\r\n" +
								"        </calculatedAttribute>\r\n" +
								"        <calculatedAttribute name=\"DUMMYDOUBLE\" datatype=\"double\" isViewAttribute=\"true\">\r\n" +
								"          <formula>strlen(\"DUMMY\") * 1.5</formula>\r\n" +
								"        </calculatedAttribute>\r\n" +
								"        <calculatedAttribute name=\"DUMMYINT\" datatype=\"int\" isViewAttribute=\"true\">\r\n" +
								"          <formula>strlen(\"DUMMY\") + 1</formula>\r\n" +
								"        </calculatedAttribute>\r\n" +
								"      </attributes>\r\n" +
								"    </projection>\r\n" +
								"  </calculationViews>\r\n" +
								"</calculationScenario>\r\n" +
								"</cubeSchema>'" ) ) {
					ps.execute();
				}

				try ( PreparedStatement ps = connection
						.prepareStatement(
								"CREATE COLUMN VIEW " + CALC_VIEW_NAME + " TYPE CALCULATION WITH PARAMETERS ('PARENTCALCINDEX'='" + CALC_SCENARIO_NAME
										+ "', 'PARENTCALCNODE'='" + PROJECTION_NODE_NAME + "')" ) ) {
					ps.execute();
				}
			} );
		} );
	}

	@Override
	protected void releaseSessionFactory() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection.prepareStatement( "DROP CALCULATION SCENARIO " + CALC_SCENARIO_NAME + " CASCADE" ) ) {
					ps.execute();
				}
				catch (Exception e) {
					// Ignore
				}
			} );
		} );
		super.releaseSessionFactory();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ CVEntity.class };
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Test
	@JiraKey(value = "HHH-12541")
	public void testCalcViewEntity() throws Exception {
		Session s = openSession();
		CVEntity cvEntity = s.find( CVEntity.class, "X" );
		assertEquals( "X", cvEntity.getDummy() );
		assertEquals( "XX", cvEntity.getDummydummy() );
		assertEquals( 2, cvEntity.getDummyint() );
		assertEquals( 1.5, cvEntity.getDummydouble(), 0.1 );
	}

	@Test
	@JiraKey(value = "HHH-12541")
	public void testCalcViewEntityQuery() throws Exception {
		Session s = openSession();
		Query<CVEntity> query = s.createQuery( "select e from CVEntity e", CVEntity.class );
		List<CVEntity> list = query.list();
		assertEquals( 1, list.size() );
		CVEntity cvEntity = list.get( 0 );
		assertEquals( "X", cvEntity.getDummy() );
		assertEquals( "XX", cvEntity.getDummydummy() );
		assertEquals( 2, cvEntity.getDummyint() );
		assertEquals( 1.5, cvEntity.getDummydouble(), 0.1 );
	}

	@Test
	@JiraKey(value = "HHH-12541")
	public void testCalcViewEntityCriteriaQuery() throws Exception {
		Session s = openSession();
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<CVEntity> criteriaQuery = criteriaBuilder.createQuery( CVEntity.class );
		Root<CVEntity> from = criteriaQuery.from( CVEntity.class );
		criteriaQuery.where( criteriaBuilder.equal( from.get( "dummydummy" ), "XX" ) );
		Query<CVEntity> query = this.session.createQuery( criteriaQuery );
		List<CVEntity> list = query.list();
		assertEquals( 1, list.size() );
		CVEntity cvEntity = list.get( 0 );
		assertEquals( "X", cvEntity.getDummy() );
		assertEquals( "XX", cvEntity.getDummydummy() );
		assertEquals( 2, cvEntity.getDummyint() );
		assertEquals( 1.5, cvEntity.getDummydouble(), 0.1 );
	}

	@Test
	@JiraKey(value = "HHH-12541")
	public void testCalcViewDTO() throws Exception {
		Session s = openSession();
		Query<CVEntityDTO> query = s.createQuery(
				"select new " + CVEntityDTO.class.getName() + "(e.dummyint, e.dummy) from CVEntity e",
				CVEntityDTO.class );
		List<CVEntityDTO> list = query.list();
		assertEquals( 1, list.size() );
		CVEntityDTO cvEntity = list.get( 0 );
		assertEquals( "X", cvEntity.getDummy() );
		assertEquals( 2, cvEntity.getDummyint() );
	}

	@Entity(name = "CVEntity")
	@Table(name = CALC_VIEW_NAME)
	private static class CVEntity {

		private String dummydummy;
		private double dummydouble;
		private int dummyint;
		@Id
		private String dummy;

		public String getDummydummy() {
			return this.dummydummy;
		}

		public double getDummydouble() {
			return this.dummydouble;
		}

		public int getDummyint() {
			return this.dummyint;
		}

		public String getDummy() {
			return this.dummy;
		}

	}

	private static class CVEntityDTO {

		private int dummyint;
		private String dummy;

		@SuppressWarnings("unused")
		public CVEntityDTO(int dummyint, String dummy) {
			this.dummyint = dummyint;
			this.dummy = dummy;
		}

		public int getDummyint() {
			return this.dummyint;
		}

		public String getDummy() {
			return this.dummy;
		}

	}
}
