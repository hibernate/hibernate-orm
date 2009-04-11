//$Id$
package org.hibernate.test.annotations.xml.ejb3;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class Ejb3XmlTest extends TestCase {
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

	protected Class[] getMappings() {
		return new Class[]{
				CarModel.class,
				Manufacturer.class,
				Model.class,
				Light.class
				//Lighter.class xml only entuty
		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[]{
				"org/hibernate/test/annotations/xml/ejb3/orm.xml",
				"org/hibernate/test/annotations/xml/ejb3/orm2.xml",
				"org/hibernate/test/annotations/xml/ejb3/orm3.xml"
		};
	}
}
