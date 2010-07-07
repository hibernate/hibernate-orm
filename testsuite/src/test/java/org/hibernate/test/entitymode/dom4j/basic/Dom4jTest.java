// $Id: Dom4jTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.entitymode.dom4j.basic;

import java.util.Map;

import junit.framework.Test;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Example;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.transform.Transformers;
import org.hibernate.util.XMLHelper;

/**
 * @author Gavin King
 */
public class Dom4jTest extends FunctionalTestCase {

	public Dom4jTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] {
				"entitymode/dom4j/basic/Account.hbm.xml",
				"entitymode/dom4j/basic/AB.hbm.xml",
				"entitymode/dom4j/basic/Employer.hbm.xml"
		};
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.DEFAULT_ENTITY_MODE, EntityMode.DOM4J.toString() );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( Dom4jTest.class );
	}

// TODO : still need to figure out inheritence support within the DOM4J entity-mode
//
//	public void testSubtyping() throws Exception {
//		Element employer = DocumentFactory.getInstance().createElement( "employer" );
//		employer.addAttribute( "name", "JBoss" );
//		Element gavin = employer.addElement( "techie" );
//		gavin.addAttribute( "name", "Gavin" );
//		Element ben = employer.addElement( "sales-dude" );
//		ben.addAttribute( "name", "Ben" );
//		print( employer );
//
//		Session s = openSession();
//		Transaction t = s.beginTransaction();
//		s.persist( "Employer", employer );
//		Long eid = new Long( employer.attributeValue( "id" ) );
//		t.commit();
//		s.close();
//
//		s = openSession();
//		t = s.beginTransaction();
//		employer = (Element) s.get( "Employer", eid );
//		print( employer );
//		s.delete( "Employer", employer );
//		t.commit();
//		s.close();
//
//		Element dept = DocumentFactory.getInstance().createElement( "department" );
//		dept.addAttribute( "name", "engineering" );
//		Element steve = dept.addElement( "manager" ).addElement( "techie" );
//		steve.addAttribute( "name", "Steve" );
//		print( dept );
//
//		s = openSession();
//		t = s.beginTransaction();
//		s.persist( "Department", dept );
//		Long did = new Long( dept.attributeValue( "id" ) );
//		t.commit();
//		s.close();
//
//		s = openSession();
//		t = s.beginTransaction();
//		dept = ( Element ) s.load( "Department", did );
//		print( dept );
//		s.delete( "Department", dept );
//		t.commit();
//		s.close();
//	}
	
	public void testCompositeId() throws Exception {
		Element a = DocumentFactory.getInstance().createElement( "a" );
		a.addAttribute("id", "1");
		a.addElement("x").setText("foo bar");
		//Element bs = a.addElement("bs");
		Element b = a.addElement("b");
		//b.addElement("bId").setText("1");
		//b.addElement("aId").setText("1");
		b.addAttribute("bId", "1");
		b.addAttribute("aId", "1");
		b.setText("foo foo");
		b = a.addElement("b");
		//b.addElement("bId").setText("2");
		//b.addElement("aId").setText("1");
		b.addAttribute("bId", "2");
		b.addAttribute("aId", "1");
		b.setText("bar bar");
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist("A", a);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		a = (Element) s.createCriteria("A").uniqueResult();
		assertEquals( a.elements("b").size(), 2 );
		print(a);
		s.delete("A", a);
		t.commit();
		s.close();
	}

	public void testDom4j() throws Exception {
		Element acct = DocumentFactory.getInstance().createElement( "account" );
		acct.addAttribute( "id", "abc123" );
		acct.addElement( "balance" ).setText( "123.45" );
		Element cust = acct.addElement( "customer" );
		cust.addAttribute( "id", "xyz123" );
		Element foo1 = cust.addElement( "stuff" ).addElement( "foo" );
		foo1.setText( "foo" );
		foo1.addAttribute("bar", "x");
		Element foo2 = cust.element( "stuff" ).addElement( "foo" );
		foo2.setText( "bar" );
		foo2.addAttribute("bar", "y");
		cust.addElement( "amount" ).setText( "45" );
		cust.setText( "An example customer" );
		Element name = cust.addElement( "name" );
		name.addElement( "first" ).setText( "Gavin" );
		name.addElement( "last" ).setText( "King" );

		Element loc = DocumentFactory.getInstance().createElement( "location" );
		loc.addElement( "address" ).setText( "Karbarook Avenue" );

		print( acct );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( "Location", loc );
		cust.addElement( "location" ).addAttribute( "id", loc.attributeValue( "id" ) );
		s.persist( "Account", acct );
		t.commit();
		s.close();

		print( loc );

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.get( "Customer", "xyz123" );
		print( cust );
		acct = (Element) s.get( "Account", "abc123" );
		print( acct );
		assertEquals( acct.element( "customer" ), cust );
		cust.element( "name" ).element( "first" ).setText( "Gavin A" );
		Element foo3 = cust.element("stuff").addElement("foo");
		foo3.setText("baz");
		foo3.addAttribute("bar", "z");
		cust.element("amount").setText("3");
		cust.addElement("amount").setText("56");
		t.commit();
		s.close();

		System.out.println();

		acct.element( "balance" ).setText( "3456.12" );
		cust.addElement( "address" ).setText( "Karbarook Ave" );

		assertEquals( acct.element( "customer" ), cust );

		cust.setText( "Still the same example!" );

		s = openSession();
		t = s.beginTransaction();
		s.saveOrUpdate( "Account", acct );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.get( "Customer", "xyz123" );
		print( cust );
		acct = (Element) s.get( "Account", "abc123" );
		print( acct );
		assertEquals( acct.element( "customer" ), cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.createCriteria( "Customer" )
				.add( Example.create( cust ) )
				.uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		acct = (Element) s.createQuery( "from Account a left join fetch a.customer" )
				.uniqueResult();
		print( acct );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		Map m = (Map) s.createQuery( "select a as acc from Account a left join fetch a.customer" )
			.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).uniqueResult();
		acct = (Element)m.get("acc"); 
		print( acct );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		acct = (Element) s.createQuery( "from Account" ).uniqueResult();
		print( acct );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.createQuery( "from Customer c left join fetch c.stuff" ).uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.createQuery( "from Customer c left join fetch c.morestuff" ).uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.createQuery( "from Customer c left join fetch c.morestuff" ).uniqueResult();
		print( cust );
		cust = (Element) s.createQuery( "from Customer c left join fetch c.stuff" ).uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.createQuery( "from Customer c left join fetch c.accounts" ).uniqueResult();
		Element a1 = cust.element( "accounts" ).addElement( "account" );
		a1.addElement( "balance" ).setText( "12.67" );
		a1.addAttribute( "id", "lkj345" );
		a1.addAttribute("acnum", "0");
		Element a2 = cust.element( "accounts" ).addElement( "account" );
		a2.addElement( "balance" ).setText( "10000.00" );
		a2.addAttribute( "id", "hsh987" );
		a2.addAttribute("acnum", "1");
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		t = s.beginTransaction();
		cust = (Element) s.createQuery( "from Customer c left join fetch c.accounts" ).uniqueResult();
		print( cust );
		t.commit();
		s.close();

		// clean up
		s = openSession();
		t = s.beginTransaction();
		s.delete( "Account", acct );
		s.delete( "Location", loc );
		s.createQuery( "delete from Account" ).executeUpdate();
		t.commit();
		s.close();
	}

	public void testMapIndexEmision() throws Throwable {
		Element acct = DocumentFactory.getInstance().createElement( "account" );
		acct.addAttribute( "id", "abc123" );
		acct.addElement( "balance" ).setText( "123.45" );
		Element cust = acct.addElement( "customer" );
		cust.addAttribute( "id", "xyz123" );
		Element foo1 = cust.addElement( "stuff" ).addElement( "foo" );
		foo1.setText( "foo" );
		foo1.addAttribute("bar", "x");
		Element foo2 = cust.element( "stuff" ).addElement( "foo" );
		foo2.setText( "bar" );
		foo2.addAttribute("bar", "y");
		cust.addElement( "amount" ).setText( "45" );
		cust.setText( "An example customer" );
		Element name = cust.addElement( "name" );
		name.addElement( "first" ).setText( "Gavin" );
		name.addElement( "last" ).setText( "King" );

		print( acct );

		Element loc = DocumentFactory.getInstance().createElement( "location" );
		loc.addElement( "address" ).setText( "Karbarook Avenue" );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( "Location", loc );
		cust.addElement( "location" ).addAttribute( "id", loc.attributeValue( "id" ) );
		s.persist( "Account", acct );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		cust = ( Element ) s.get( "Customer", "xyz123" );
		print( cust );
		assertEquals( "Incorrect stuff-map size", 2, cust.element( "stuff" ).elements( "foo" ).size() );
		Element stuffElement = ( Element ) cust.element( "stuff" ).elements(  "foo" ).get( 0 );
		assertNotNull( "No map-key value present", stuffElement.attribute( "bar" ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( "Account", acct );
		s.delete( "Location", loc );
		t.commit();
		s.close();
	}

	public static void print(Element elt) throws Exception {
		XMLHelper.dump( elt );
	}
}
