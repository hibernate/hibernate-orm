/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytoonewithformula;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.TeradataDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sharath Reddy
 */
public class ManyToOneWithFormulaTest extends BaseCoreFunctionalTestCase {
	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	@SkipForDialect( value = { HSQLDialect.class }, comment = "The used join conditions does not work in HSQLDB. See HHH-4497." )
	@SkipForDialect( value = { SQLServer2005Dialect.class } )
	@SkipForDialect( value = { Oracle8iDialect.class }, comment = "Oracle/DB2 do not support 'substring' function" )
	@SkipForDialect( value = { DB2Dialect.class }, comment = "Oracle/DB2 do not support 'substring' function" )
	@SkipForDialect( value = { TeradataDialect.class }, comment = "Teradata doesn't support substring(?,?,?). \"substr\" would work." )
	public void testManyToOneFromNonPkToNonPk() throws Exception {
		// also tests usage of the stand-alone @JoinFormula annotation (i.e. not wrapped within @JoinColumnsOrFormulas)
		Session s = openSession();
		Transaction tx = s.beginTransaction();

        Product kit = new Product();
        kit.id = 1;
        kit.productIdnf = "KIT";
        kit.description = "Kit";
        s.persist(kit);

        Product kitkat = new Product();
        kitkat.id = 2;
        kitkat.productIdnf = "KIT_KAT";
        kitkat.description = "Chocolate";
        s.persist(kitkat);

        s.flush();
        s.clear();

        kit = (Product) s.get(Product.class, 1);
        kitkat = (Product) s.get(Product.class, 2);
        System.out.println(kitkat.description);
        assertNotNull(kitkat);
        assertEquals(kit, kitkat.getProductFamily());
        assertEquals(kit.productIdnf, kitkat.getProductFamily().productIdnf);
        assertEquals("KIT_KAT", kitkat.productIdnf.trim());
        assertEquals("Chocolate", kitkat.description.trim());

        tx.rollback();
		s.close();
    }

    @Test
    @RequiresDialect(value = {SQLServer2005Dialect.class})
    public void testManyToOneFromNonPkToNonPkSqlServer() throws Exception {
        // also tests usage of the stand-alone @JoinFormula annotation (i.e. not wrapped within @JoinColumnsOrFormulas)
        Session s = openSession();
        Transaction tx = s.beginTransaction();

        ProductSqlServer kit = new ProductSqlServer();
        kit.id = 1;
        kit.productIdnf = "KIT";
        kit.description = "Kit";
        s.persist(kit);

        ProductSqlServer kitkat = new ProductSqlServer();
        kitkat.id = 2;
        kitkat.productIdnf = "KIT_KAT";
        kitkat.description = "Chocolate";
        s.persist(kitkat);

        s.flush();
        s.clear();

        kit = (ProductSqlServer) s.get(ProductSqlServer.class, 1);
        kitkat = (ProductSqlServer) s.get(ProductSqlServer.class, 2);
        System.out.println(kitkat.description);
        assertNotNull(kitkat);
        assertEquals(kit, kitkat.getProductFamily());
        assertEquals(kit.productIdnf, kitkat.getProductFamily().productIdnf);
        assertEquals("KIT_KAT", kitkat.productIdnf.trim());
        assertEquals("Chocolate", kitkat.description.trim());

        tx.rollback();
        s.close();
    }

	@Override
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
				ManufacturerId.class,
				Product.class,
                ProductSqlServer.class
		};
	}

}
