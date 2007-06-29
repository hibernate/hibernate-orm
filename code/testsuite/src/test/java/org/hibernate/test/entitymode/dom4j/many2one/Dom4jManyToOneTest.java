package org.hibernate.test.entitymode.dom4j.many2one;

import java.util.List;

import junit.framework.Test;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Paco Hernández
 */
public class Dom4jManyToOneTest extends FunctionalTestCase {

	public Dom4jManyToOneTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "entitymode/dom4j/many2one/Car.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( Dom4jManyToOneTest.class );
	}
	
	public void testDom4jOneToMany() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		CarType carType = new CarType();
		carType.setTypeName("Type 1");
		s.save(carType);

		Car car = new Car();
		car.setCarType(carType);
		car.setModel("Model 1");
		s.save(car);
		
		CarPart carPart1 = new CarPart();
		carPart1.setPartName("chassis");
		car.getCarParts().add(carPart1);
		
		t.commit();
		s.close();

		s = openSession();
		Session dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();

		Element element = (Element) dom4jSession.createQuery( "from Car c join fetch c.carParts" ).uniqueResult();

		String expectedResult = "<car id=\"" + 
			car.getId() + 
			"\"><carPart>" + 
			carPart1.getId() +
			"</carPart><model>Model 1</model><carType id=\"" +
			carType.getId() +
			"\"><typeName>Type 1</typeName></carType></car>";
				
		print(element);
		assertTrue(element.asXML().equals(expectedResult));
		
		s.createQuery("delete from CarPart").executeUpdate();
		s.createQuery("delete from Car").executeUpdate();
		s.createQuery("delete from CarType").executeUpdate();
		
		t.commit();
		s.close();
	}

	public void testDom4jManyToOne() throws Exception {

		Session s = openSession();
		Transaction t = s.beginTransaction();

		CarType carType = new CarType();
		carType.setTypeName("Type 1");
		s.save(carType);

		Car car1 = new Car();
		car1.setCarType(carType);
		car1.setModel("Model 1");
		s.save(car1);
		
		Car car2 = new Car();
		car2.setCarType(carType);
		car2.setModel("Model 2");
		s.save(car2);
		
		t.commit();
		s.close();

		s = openSession();
		Session dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();

		List list = dom4jSession.createQuery( "from Car c join fetch c.carType order by c.model asc" ).list();

		String[] expectedResults = new String[] {
				"<car id=\"" + 
				car1.getId() +
				"\"><model>Model 1</model><carType id=\"" + 
				carType.getId() +
				"\"><typeName>Type 1</typeName></carType></car>",
				"<car id=\"" + 
				car2.getId() +
				"\"><model>Model 2</model><carType id=\"" +
				carType.getId() +
				"\"><typeName>Type 1</typeName></carType></car>"
		};
				
		for (int i = 0; i < list.size(); i++) {
			Element element = (Element) list.get(i);

			print(element);
			assertTrue(element.asXML().equals(expectedResults[i]));
		}
		
		s.createQuery("delete from Car").executeUpdate();
		s.createQuery("delete from CarType").executeUpdate();
		
		t.commit();
		s.close();
	}

	public static void print(Element elt) throws Exception {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		// outformat.setEncoding(aEncodingScheme);
		XMLWriter writer = new XMLWriter( System.out, outformat );
		writer.write( elt );
		writer.flush();
		// System.out.println( elt.asXML() );
	}
}
