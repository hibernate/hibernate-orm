/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Sharath Reddy
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ManyToOneWithFormulaTest {
	@Test
	@DomainModel( annotatedClasses = { Menu.class, FoodItem.class } )
	@SessionFactory
	public void testManyToOneFromNonPk(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Menu menu = new Menu();
			menu.setOrderNbr( "123" );
			menu.setIsDefault( "F" );
			session.persist( menu );
			FoodItem foodItem = new FoodItem();
			foodItem.setItem( "Mouse" );
			foodItem.setOrder( menu );
			session.persist( foodItem );
			session.flush();
			session.clear();

			foodItem = session.get( FoodItem.class, foodItem.getId() );
			assertThat( foodItem.getOrder() ).isNotNull();
			assertThat( foodItem.getOrder().getOrderNbr() ).isEqualTo( "123" );

			session.getTransaction().markRollbackOnly();
		} );
	}

	@Test
	@DomainModel( annotatedClasses = { Company.class, Person.class } )
	@SessionFactory
	public void testManyToOneFromPk(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Company company = new Company();
			session.persist( company );

			Person person = new Person();
			person.setDefaultFlag( "T" );
			person.setCompanyId( company.getId() );
			session.persist( person );

			session.flush();
			session.clear();

			company = session.get( Company.class, company.getId() );
			assertThat( company.getDefaultContactPerson() ).isNotNull();
			assertThat( company.getDefaultContactPerson().getId() ).isEqualTo( person.getId() );

			session.getTransaction().markRollbackOnly();
		} );
	}

	@Test
	@DomainModel( annotatedClasses = { Message.class, Language.class } )
	@SessionFactory
	@SkipForDialect( dialectClass = HSQLDialect.class, reason = "The used join conditions does not work in HSQLDB. See HHH-4497" )
	public void testManyToOneToPkWithOnlyFormula(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
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
			assertThat( msg.getLanguage() ).isNotNull();
			assertThat( msg.getLanguage().getCode() ).isEqualTo( "EN" );

			session.getTransaction().markRollbackOnly();
		} );
	}

	@Test
	@DomainModel( annotatedClasses = {
			Contract.class,
			ContractId.class,
			Model.class,
			ModelId.class,
			Manufacturer.class,
			ManufacturerId.class,
	} )
	@SessionFactory
	public void testReferencedColumnNameBelongsToEmbeddedIdOfReferencedEntity(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Integer companyCode = 10;
			Integer mfgCode = 100;
			String contractNumber = "NSAR97841";
			ContractId contractId = new ContractId(companyCode, 12457L, 1);

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

			contract = session.get( Contract.class, contractId );
			assertThat( contract.getContractNumber() ).isEqualTo( "NSAR97841" );
			assertThat( contract.getManufacturer().getName() ).isEqualTo( "FORD" );
			assertThat( contract.getModel().getName() ).isEqualTo( "FORD FOCUS" );

			session.getTransaction().markRollbackOnly();
		} );
	}

	@Test
	@DomainModel( annotatedClasses = Product.class )
	@SessionFactory
	@SkipForDialect( dialectClass =HSQLDialect.class, reason = "The used join conditions does not work in HSQLDB. See HHH-4497." )
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "Oracle do not support 'substring' function" )
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = " Altibase char type returns with trailing spaces")
	public void testManyToOneFromNonPkToNonPk(SessionFactoryScope scope) {
		// also tests usage of the stand-alone @JoinFormula annotation
		// (i.e. not wrapped within @JoinColumnsOrFormulas)

		scope.inTransaction( (session) -> {
			Product kit = new Product();
			kit.id = 1;
			kit.productIdnf = "KIT";
			kit.description = "Kit";
			session.persist(kit);

			Product kitkat = new Product();
			kitkat.id = 2;
			kitkat.productIdnf = "KIT_KAT";
			kitkat.description = "Chocolate";
			session.persist(kitkat);

			session.flush();
			session.clear();

			kit = session.get(Product.class, 1);
			kitkat = session.get(Product.class, 2);

			assertThat( kitkat ).isNotNull();
			assertThat( kitkat.getProductFamily() ).isEqualTo( kit );
			assertThat( kit.productIdnf ).isEqualTo( kitkat.getProductFamily().productIdnf );
			assertThat( kitkat.productIdnf.trim() ).isEqualTo( "KIT_KAT" );
			assertThat( kitkat.description.trim() ).isEqualTo( "Chocolate" );

			session.getTransaction().markRollbackOnly();
		} );
	}

	@Test
	@DomainModel( annotatedClasses = ProductSqlServer.class )
	@SessionFactory
	@RequiresDialect( SQLServerDialect.class )
	public void testManyToOneFromNonPkToNonPkSqlServer(SessionFactoryScope scope) {
		// also tests usage of the stand-alone @JoinFormula annotation
		// (i.e. not wrapped within @JoinColumnsOrFormulas)

		scope.inTransaction( (session) -> {
			ProductSqlServer kit = new ProductSqlServer();
			kit.id = 1;
			kit.productIdnf = "KIT";
			kit.description = "Kit";
			session.persist(kit);

			ProductSqlServer kitkat = new ProductSqlServer();
			kitkat.id = 2;
			kitkat.productIdnf = "KIT_KAT";
			kitkat.description = "Chocolate";
			session.persist(kitkat);

			session.flush();
			session.clear();

			kit = session.get(ProductSqlServer.class, 1);
			kitkat = session.get(ProductSqlServer.class, 2);

			assertThat( kitkat ).isNotNull();
			assertThat( kitkat.getProductFamily() ).isEqualTo( kit );
			assertThat( kit.productIdnf ).isEqualTo( kitkat.getProductFamily().productIdnf );
			assertThat( kitkat.productIdnf.trim() ).isEqualTo( "KIT_KAT" );
			assertThat( kitkat.description.trim() ).isEqualTo( "Chocolate" );

			session.getTransaction().markRollbackOnly();
		} );
	}

}
