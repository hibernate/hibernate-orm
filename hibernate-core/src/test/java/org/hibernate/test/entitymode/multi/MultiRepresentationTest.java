/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.entitymode.multi;
import java.sql.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.EntityMode;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Implementation of MultiRepresentationTest.
 *
 * @author Steve Ebersole
 */
public class MultiRepresentationTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "entitymode/multi/Stock.hbm.xml", "entitymode/multi/Valuation.hbm.xml" };
	}

	@Test
	public void testPojoRetreival() {
		TestData testData = new TestData();
		testData.create();

		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Stock stock = ( Stock ) session.get( Stock.class, new Long( 1 ) );
		assertEquals( "Something wrong!", new Long( 1 ), stock.getId() );

		txn.commit();
		session.close();

		testData.destroy();
	}

	@Test
	public void testDom4jRetreival() {
		TestData testData = new TestData();
		testData.create();

		Session session = openSession();
		Transaction txn = session.beginTransaction();
		org.hibernate.Session dom4j = session.getSession( EntityMode.DOM4J );

		Object rtn = dom4j.get( Stock.class.getName(), testData.stockId );
		Element element = ( Element ) rtn;

		assertEquals( "Something wrong!", testData.stockId, Long.valueOf( element.attributeValue( "id" ) ) );

		System.out.println( "**** XML: ****************************************************" );
		prettyPrint( element );
		System.out.println( "**************************************************************" );

		Element currVal = element.element( "currentValuation" );

		System.out.println( "**** XML: ****************************************************" );
		prettyPrint( currVal );
		System.out.println( "**************************************************************" );

		txn.rollback();
		session.close();

		testData.destroy();
	}

	@Test
	public void testDom4jSave() {
		TestData testData = new TestData();
		testData.create();

		Session pojos = openSession();
		Transaction txn = pojos.beginTransaction();
		org.hibernate.Session dom4j = pojos.getSession( EntityMode.DOM4J );

		Element stock = DocumentFactory.getInstance().createElement( "stock" );
		stock.addElement( "tradeSymbol" ).setText( "IBM" );

		Element val = stock.addElement( "currentValuation" ).addElement( "valuation" );
		val.appendContent( stock );
		val.addElement( "valuationDate" ).setText( new java.util.Date().toString() );
		val.addElement( "value" ).setText( "121.00" );

		dom4j.save( Stock.class.getName(), stock );
		dom4j.flush();

		txn.rollback();
		pojos.close();

		assertTrue( !pojos.isOpen() );
		assertTrue( !dom4j.isOpen() );

		prettyPrint( stock );

		testData.destroy();
	}

	@Test
	public void testDom4jHQL() {
		TestData testData = new TestData();
		testData.create();

		Session session = openSession();
		Transaction txn = session.beginTransaction();
		org.hibernate.Session dom4j = session.getSession( EntityMode.DOM4J );

		List result = dom4j.createQuery( "from Stock" ).list();

		assertEquals( "Incorrect result size", 1, result.size() );
		Element element = ( Element ) result.get( 0 );
		assertEquals( "Something wrong!", testData.stockId, Long.valueOf( element.attributeValue( "id" ) ) );

		System.out.println( "**** XML: ****************************************************" );
		prettyPrint( element );
		System.out.println( "**************************************************************" );

		txn.rollback();
		session.close();

		testData.destroy();
	}

	private class TestData {
		private Long stockId;

		private void create() {
			Session session = getSessions().openSession();
			session.beginTransaction();
			Stock stock = new Stock();
			stock.setTradeSymbol( "JBOSS" );
			Valuation valuation = new Valuation();
			valuation.setStock( stock );
			valuation.setValuationDate( new Date( new java.util.Date().getTime() ) );
			valuation.setValue( new Double( 200.0 ) );
			stock.setCurrentValuation( valuation );
			stock.getValuations().add( valuation );

			session.save( stock );
			session.save( valuation );

			session.getTransaction().commit();
			session.close();

			stockId = stock.getId();
		}

		private void destroy() {
			Session session = getSessions().openSession();
			session.beginTransaction();
			Iterator stocks = session.createQuery( "from Stock" ).list().iterator();
			while ( stocks.hasNext() ) {
				final Stock stock = ( Stock ) stocks.next();
				stock.setCurrentValuation( null );
				session.flush();
				Iterator valuations = stock.getValuations().iterator();
				while ( valuations.hasNext() ) {
					session.delete( valuations.next() );
				}
				session.delete( stock );
			}
			session.getTransaction().commit();
			session.close();
		}
	}

	private void prettyPrint(Element element) {
		//System.out.println( element.asXML() );
		try {
			OutputFormat format = OutputFormat.createPrettyPrint();
			new XMLWriter( System.out, format ).write( element );
			System.out.println();
		}
		catch ( Throwable t ) {
			System.err.println( "Unable to pretty print element : " + t );
		}
	}
}
