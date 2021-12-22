/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;

import org.hibernate.dialect.HSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Sharath Reddy
 */
@DomainModel(
		annotatedClasses = {
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
		}
)
@SessionFactory
public class ManyToOneWithFormulaTest {

	@Test
	public void testManyToOneFromNonPk(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Menu menu = new Menu();
					menu.setOrderNbr( "123" );
					menu.setDefault( "F" );
					session.persist( menu );
					FoodItem foodItem = new FoodItem();
					foodItem.setItem( "Mouse" );
					foodItem.setOrder( menu );
					session.persist( foodItem );
					session.flush();
					session.clear();
					foodItem = session.get( FoodItem.class, foodItem.getId() );
					assertNotNull( foodItem.getOrder() );
					assertEquals( "123", foodItem.getOrder().getOrderNbr() );
				}
		);
	}

	@Test
	public void testManyToOneFromPk(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Company company = new Company();
					session.persist( company );

					Person person = new Person();
					person.setDefaultFlag( "T" );
					person.setCompanyId( company.getId() );
					session.persist( person );

					session.flush();
					session.clear();

					company = session.get( Company.class, company.getId() );
					assertNotNull( company.getDefaultContactPerson() );
					assertEquals( person.getId(), company.getDefaultContactPerson().getId() );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "The used join conditions does not work in HSQLDB. See HHH-4497")
	public void testManyToOneToPkWithOnlyFormula(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Language language = new Language();
					language.setCode( "EN" );
					language.setName( "English" );
					session.persist( language );

					Message msg = new Message();
					msg.setLanguageCode( "en" );
					msg.setLanguageName( "English" );
					session.persist( msg );

					session.flush();
					session.clear();

					msg = session.get( Message.class, msg.getId() );
					assertNotNull( msg.getLanguage() );
					assertEquals( "EN", msg.getLanguage().getCode() );
				}
		);
	}

	@Test
	public void testReferencedColumnNameBelongsToEmbeddedIdOfReferencedEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Integer companyCode = 10;
					Integer mfgCode = 100;
					String contractNumber = "NSAR97841";
					ContractId contractId = new ContractId( companyCode, 12457l, 1 );

					Manufacturer manufacturer = new Manufacturer( new ManufacturerId(
							companyCode, mfgCode ), "FORD" );

					Model model = new Model(
							new ModelId( companyCode, mfgCode, "FOCUS" ),
							"FORD FOCUS"
					);

					session.persist( manufacturer );
					session.persist( model );

					Contract contract = new Contract();
					contract.setId( contractId );
					contract.setContractNumber( contractNumber );
					contract.setManufacturer( manufacturer );
					contract.setModel( model );

					session.persist( contract );

					session.flush();
					session.clear();

					contract = session.load( Contract.class, contractId );
					assertEquals( "NSAR97841", contract.getContractNumber() );
					assertEquals( "FORD", contract.getManufacturer().getName() );
					assertEquals( "FORD FOCUS", contract.getModel().getName() );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "The used join conditions does not work in HSQLDB. See HHH-4497.")
	public void testManyToOneFromNonPkToNonPk(SessionFactoryScope scope) {
		// also tests usage of the stand-alone @JoinFormula annotation (i.e. not wrapped within @JoinColumnsOrFormulas)
		scope.inTransaction(
				session -> {
					Product kit = new Product();
					kit.id = 1;
					kit.productIdnf = "KIT";
					kit.description = "Kit";
					session.persist( kit );

					Product kitkat = new Product();
					kitkat.id = 2;
					kitkat.productIdnf = "KIT_KAT";
					kitkat.description = "Chocolate";
					session.persist( kitkat );

					session.flush();
					session.clear();

					kit = session.get( Product.class, 1 );
					kitkat = session.get( Product.class, 2 );
					System.out.println( kitkat.description );
					assertNotNull( kitkat );
					assertEquals( kit, kitkat.getProductFamily() );
					assertEquals( kit.productIdnf, kitkat.getProductFamily().productIdnf );
					assertEquals( "KIT_KAT", kitkat.productIdnf.trim() );
					assertEquals( "Chocolate", kitkat.description.trim() );
				}
		);
	}

	@Test
	public void testManyToOneFromNonPkToNonPkSqlServer(SessionFactoryScope scope) {
		// also tests usage of the stand-alone @JoinFormula annotation (i.e. not wrapped within @JoinColumnsOrFormulas)
		scope.inTransaction(
				session -> {
					ProductSqlServer kit = new ProductSqlServer();
					kit.id = 1;
					kit.productIdnf = "KIT";
					kit.description = "Kit";
					session.persist( kit );

					ProductSqlServer kitkat = new ProductSqlServer();
					kitkat.id = 2;
					kitkat.productIdnf = "KIT_KAT";
					kitkat.description = "Chocolate";
					session.persist( kitkat );

					session.flush();
					session.clear();

					kit = session.get( ProductSqlServer.class, 1 );
					kitkat = session.get( ProductSqlServer.class, 2 );
					System.out.println( kitkat.description );
					assertNotNull( kitkat );
					assertEquals( kit, kitkat.getProductFamily() );
					assertEquals( kit.productIdnf, kitkat.getProductFamily().productIdnf );
					assertEquals( "KIT_KAT", kitkat.productIdnf.trim() );
					assertEquals( "Chocolate", kitkat.description.trim() );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from ProductSqlServer" ).executeUpdate();
					session.createQuery( "delete from Product" ).executeUpdate();
					session.createQuery( "delete from Contract" ).executeUpdate();
					session.createQuery( "delete from Model" ).executeUpdate();
					session.createQuery( "delete from Manufacturer" ).executeUpdate();
					session.createQuery( "delete from Message" ).executeUpdate();
					session.createQuery( "delete from Language" ).executeUpdate();
					session.createQuery( "delete from Company" ).executeUpdate();
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from Menu" ).executeUpdate();
					session.createQuery( "delete from FoodItem" ).executeUpdate();
				}
		);
	}

}
