/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.persister.collection.BasicCollectionPersister;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class Ejb3XmlTest extends BaseCoreFunctionalTestCase {
	@Test
	@SkipForDialect(value = {PostgreSQL81Dialect.class, PostgreSQLDialect.class},
			comment = "postgresql jdbc driver does not implement the setQueryTimeout method")
	@SkipForDialect(value = TeradataDialect.class,
			jiraKey = "HHH-8190",
			comment = "uses Teradata reserved word - year")
	public void testEjb3Xml() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		CarModel model = new CarModel();
		model.setYear( new Date() );
		Manufacturer manufacturer = new Manufacturer();
		//s.persist( manufacturer );
		model.setManufacturer( manufacturer );
		manufacturer.getModels().add( model );
		s.persist( model );
		s.flush();
		s.clear();

		model.setYear( new Date() );
		manufacturer = (Manufacturer) s.get( Manufacturer.class, manufacturer.getId() );
		@SuppressWarnings("unchecked")
		List<Model> cars = s.getNamedQuery( "allModelsPerManufacturer" )
				.setParameter( "manufacturer", manufacturer )
				.list();
		assertEquals( 1, cars.size() );
		for ( Model car : cars ) {
			assertNotNull( car.getManufacturer() );
			s.delete( manufacturer );
			s.delete( car );
		}
		tx.rollback();
		s.close();
	}

	@Test
	public void testXMLEntityHandled() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Lighter l = new Lighter();
		l.name = "Blue";
		l.power = "400F";
		s.persist( l );
		s.flush();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testXmlDefaultOverriding() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Manufacturer manufacturer = new Manufacturer();
		s.persist( manufacturer );
		s.flush();
		s.clear();

		assertEquals( 1, s.getNamedQuery( "manufacturer.findAll" ).list().size() );
		tx.rollback();
		s.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapXMLSupport() throws Exception {
		Session s = openSession();
		SessionFactory sf = s.getSessionFactory();
		Transaction tx = s.beginTransaction();

		// Verify that we can persist an object with a couple Map mappings
		VicePresident vpSales = new VicePresident();
		vpSales.name = "Dwight";
		Company company = new Company();
		company.conferenceRoomExtensions.put( "8932", "x1234" );
		company.organization.put( "sales", vpSales );
		s.persist( company );
		s.flush();
		s.clear();

		// For the element-collection, check that the orm.xml entries are honored.
		// This includes: map-key-column/column/collection-table/join-column
		BasicCollectionPersister confRoomMeta = (BasicCollectionPersister) sf.getCollectionMetadata( Company.class.getName() + ".conferenceRoomExtensions" );
		assertEquals( "company_id", confRoomMeta.getKeyColumnNames()[0] );
		assertEquals( "phone_extension", confRoomMeta.getElementColumnNames()[0] );
		assertEquals( "room_number", confRoomMeta.getIndexColumnNames()[0] );
		assertEquals( "phone_extension_lookup", confRoomMeta.getTableName() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				CarModel.class,
				Manufacturer.class,
				Model.class,
				Light.class
				//Lighter.class xml only entuty
		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/xml/ejb3/orm.xml",
				"org/hibernate/test/annotations/xml/ejb3/orm2.xml",
				"org/hibernate/test/annotations/xml/ejb3/orm3.xml",
				"org/hibernate/test/annotations/xml/ejb3/orm4.xml"
		};
	}
}
