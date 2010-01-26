/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express 
  * copyright attribution statements applied by the authors.  
  * All third-party contributions are distributed under license by 
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to 
  * use, modify, copy, or redistribute it subject to the terms and 
  * conditions of the GNU Lesser General Public License, as published 
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of 
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public 
  * License along with this distribution; if not, write to:
  * 
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */
package org.hibernate.test.annotations.manytoonewithformula;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.junit.SkipForDialect;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Sharath Reddy
 */
public class ManyToOneWithFormulaTest extends TestCase {

	public ManyToOneWithFormulaTest(String x) {
		super( x );
	}

	public void testManyToOneFromNonPk() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Menu menu = new Menu();
		menu.setOrderNbr( "123" );
		menu.setDefault( "F" );
		s.persist( menu );
		FoodItem foodItem = new FoodItem();
		foodItem.setItem( "Mouse" );
		foodItem.setOrder( menu );
		s.persist( foodItem );
		s.flush();
		s.clear();
		foodItem = ( FoodItem ) s.get( FoodItem.class, foodItem.getId() );
		assertNotNull( foodItem.getOrder() );
		assertEquals( "123", foodItem.getOrder().getOrderNbr() );
		tx.rollback();
		s.close();
	}


	public void testManyToOneFromPk() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Company company = new Company();
		s.persist( company );

		Person person = new Person();
		person.setDefaultFlag( "T" );
		person.setCompanyId( company.getId() );
		s.persist( person );

		s.flush();
		s.clear();

		company = ( Company ) s.get( Company.class, company.getId() );
		assertNotNull( company.getDefaultContactPerson() );
		assertEquals( person.getId(), company.getDefaultContactPerson().getId() );
		tx.rollback();
		s.close();
	}

	@SkipForDialect(value = { HSQLDialect.class }, comment = "The used join conditions does not work in HSQLDB. See HHH-4497")
	public void testManyToOneToPkWithOnlyFormula() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Language language = new Language();
		language.setCode( "EN" );
		language.setName( "English" );
		s.persist( language );

		Message msg = new Message();
		msg.setLanguageCode( "en" );
		msg.setLanguageName( "English" );
		s.persist( msg );

		s.flush();
		s.clear();

		msg = ( Message ) s.get( Message.class, msg.getId() );
		assertNotNull( msg.getLanguage() );
		assertEquals( "EN", msg.getLanguage().getCode() );
		tx.rollback();
		s.close();
	}

	public void testReferencedColumnNameBelongsToEmbeddedIdOfReferencedEntity() throws Exception {
 
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		
		Integer companyCode = 10;
		Integer mfgCode = 100;
		String contractNumber = "NSAR97841";
		ContractId contractId = new ContractId(companyCode, 12457l, 1);

		Manufacturer manufacturer = new Manufacturer(new ManufacturerId(
				companyCode, mfgCode), "FORD");

		Model model = new Model(new ModelId(companyCode, mfgCode, "FOCUS"),
				"FORD FOCUS");
 
		session.persist(manufacturer);
		session.persist(model);

		Contract contract = new Contract();
		contract.setId(contractId);
		contract.setContractNumber(contractNumber);
		contract.setManufacturer(manufacturer);
		contract.setModel(model);

		session.persist(contract);

		session.flush();
		session.clear();

		contract = (Contract) session.load(Contract.class, contractId);
		assertEquals("NSAR97841", contract.getContractNumber());
		assertEquals("FORD", contract.getManufacturer().getName());
		assertEquals("FORD FOCUS", contract.getModel().getName());

		tx.commit();
		session.close();
	}
	
	
	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class[] {
				Menu.class,
				FoodItem.class,
				Company.class,
				Person.class,
				Message.class,
				Language.class,
				Contract.class,
				ContractId.class,
				Model.class,
				ModelId.class,
				Manufacturer.class,
				ManufacturerId.class
		};
	}

}
