package org.hibernate.test.annotations.collectionelement;

import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AssociationOverrideTest extends TestCase {

	public void testDottedNotation() throws Exception {
		assertTrue( SchemaUtil.isTablePresent( "Employee", getCfg() ) );
		assertTrue( "Overridden @JoinColumn fails",
				SchemaUtil.isColumnPresent( "Employee", "fld_address_fk", getCfg() ) );

		assertTrue( "Overridden @JoinTable name fails", SchemaUtil.isTablePresent( "tbl_empl_sites", getCfg() ) );
		assertTrue( "Overridden @JoinTable with default @JoinColumn fails",
				SchemaUtil.isColumnPresent( "tbl_empl_sites", "employee_id", getCfg() ) );
		assertTrue( "Overridden @JoinTable.inverseJoinColumn fails",
				SchemaUtil.isColumnPresent( "tbl_empl_sites", "to_website_fk", getCfg() ) );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ContactInfo ci = new ContactInfo();
		Address address = new Address();
		address.setCity("Boston");
		address.setCountry("USA");
		address.setState("MA");
		address.setStreet("27 School Street");
		address.setZipcode("02108");
		ci.setAddress(address);
		List<PhoneNumber> phoneNumbers = new ArrayList();
		PhoneNumber num = new PhoneNumber();
		num.setNumber(5577188);
		Employee e = new Employee();
		Collection employeeList = new ArrayList();
		employeeList.add(e);
		e.setContactInfo(ci);
		num.setEmployees(employeeList);
		phoneNumbers.add(num);
		ci.setPhoneNumbers(phoneNumbers);
		SocialTouchPoints socialPoints = new SocialTouchPoints();
		List<SocialSite> sites = new ArrayList<SocialSite>();
		SocialSite site = new SocialSite();
		site.setEmployee(employeeList);
		site.setWebsite("www.jboss.org");
		sites.add(site);
		socialPoints.setWebsite(sites);
		ci.setSocial(socialPoints);
		s.persist(e);
		tx.commit();

		tx = s.beginTransaction();
		s.clear();
		e = (Employee) s.get(Employee.class,e.getId());
		tx.commit();
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
			Employee.class,
			PhoneNumber.class,
			Address.class,
			SocialSite.class,
			SocialTouchPoints.class
		};
	}

}