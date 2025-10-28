/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import org.hibernate.Hibernate;
import org.hibernate.orm.test.annotations.Company;
import org.hibernate.orm.test.annotations.Customer;
import org.hibernate.orm.test.annotations.Discount;
import org.hibernate.orm.test.annotations.Flight;
import org.hibernate.orm.test.annotations.Passport;
import org.hibernate.orm.test.annotations.Ticket;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry( settings = @Setting( name = IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa" ) )
@DomainModel(annotatedClasses = {
		Deal.class,
		org.hibernate.orm.test.annotations.manytoone.Customer.class,
		Car.class,
		Color.class,
		Flight.class,
		Company.class,
		Customer.class,
		Discount.class,
		Ticket.class,
		Passport.class,
		Parent.class,
		Child.class,
		Node.class,
		User.class,
		DistrictUser.class,
		Order.class,
		OrderLine.class,
		Frame.class,
		Lens.class
} )
@SessionFactory
public class ManyToOneTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testEager(SessionFactoryScope factoryScope) {
		var carId = factoryScope.fromTransaction( (session) -> {
			var color = new Color();
			color.setName( "Yellow" );
			session.persist( color );
			var car = new Car();
			car.setBodyColor( color );
			session.persist( car );
			return car.getId();
		} );

		factoryScope.inTransaction( (session) -> {
			var car = session.find( Car.class, carId );
			assertNotNull( car );
			assertNotNull( car.getBodyColor() );
			assertEquals( "Yellow", car.getBodyColor().getName() );
		} );
	}

	@Test
	public void testDefaultMetadata(SessionFactoryScope factoryScope) {
		var carId = factoryScope.fromTransaction( (session) -> {
			var color = new Color();
			color.setName( "Blue" );
			session.persist( color );
			var car = new Car();
			car.setBodyColor( color );
			session.persist( car );
			return car.getId();
		} );

		factoryScope.inTransaction( (session) -> {
			var car = session.find( Car.class, carId );
			assertNotNull( car );
			assertNotNull( car.getBodyColor() );
			assertEquals( "Blue", car.getBodyColor().getName() );
		} );
	}

	@Test
	public void testCreate(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.inTransaction( (session) -> {
			Flight firstOne = new Flight();
			firstOne.setId(1L);
			firstOne.setName( "AF0101" );
			firstOne.setDuration(1000L);
			Company frenchOne = new Company();
			frenchOne.setName( "Air France" );
			firstOne.setCompany( frenchOne );

			session.persist( firstOne );
			session.persist( frenchOne );
		} );

		factoryScope.inTransaction( (session) -> {
			var flight = session.find( Flight.class, 1L );
			assertNotNull( flight.getCompany() );
			assertEquals( "Air France", flight.getCompany().getName() );
		} );
	}

	@Test
	public void testCascade(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var discount = new Discount();
			discount.setDiscount( 20.12 );
			var customer = new Customer();
			var discounts = new ArrayList<>();
			discounts.add( discount );
			customer.setName( "Quentin Tarantino" );
			discount.setOwner( customer );
			customer.setDiscountTickets( discounts );
			session.persist( discount );
		} );

		factoryScope.inTransaction( (session) -> {
			var discount = session.find( Discount.class, 1L );
			assertNotNull( discount );
			assertEquals( 20.12, discount.getDiscount(), 0.01 );
			assertNotNull( discount.getOwner() );
			var customer = new Customer();
			customer.setName( "Clooney" );
			discount.setOwner( customer );
			var discounts = new ArrayList<>();
			discounts.add( discount );
			customer.setDiscountTickets( discounts );
		} );

		factoryScope.inTransaction( (session) -> {
			var discount = session.find( Discount.class, 1L );
			assertNotNull( discount );
			assertNotNull( discount.getOwner() );
			assertEquals( "Clooney", discount.getOwner().getName() );
		} );
	}

	@Test
	public void testFetch(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var discount = new Discount();
			discount.setDiscount( 20 );
			var customer = new Customer();
			var discounts = new ArrayList<>();
			discounts.add( discount );
			customer.setName( "Quentin Tarantino" );
			discount.setOwner( customer );
			customer.setDiscountTickets( discounts );
			session.persist( discount );
		} );

		factoryScope.inTransaction( (session) -> {
			var discount = session.find( Discount.class, 1L );
			assertNotNull( discount );
			assertFalse( Hibernate.isInitialized( discount.getOwner() ) );
		} );

		factoryScope.inTransaction( (session) -> {
			var discount = session.getReference( Discount.class, 1L );
			assertNotNull( discount );
			assertFalse( Hibernate.isInitialized( discount.getOwner() ) );
		} );
	}

	@Test
	public void testCompositeFK(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(  (session) -> {
			var ppk = new ParentPk();
			ppk.firstName = "John";
			ppk.lastName = "Doe";
			var p = new Parent();
			p.age = 45;
			p.id = ppk;
			session.persist( p );
			var c = new Child();
			c.parent = p;
			session.persist( c );
		} );

		factoryScope.inTransaction( (session) -> {
			//FIXME: fix this when the small parser bug will be fixed
			var result  = session.createQuery( "from Child c where c.parent.id.lastName = :lastName" )
					.setParameter( "lastName", "Doe")
					.list();
			assertEquals( 1, result.size() );
			Child c2 = (Child) result.get( 0 );
			assertEquals( 1, c2.id );
		} );
	}

	@Test
	public void testImplicitCompositeFk(SessionFactoryScope factoryScope) {
		var node2Pk = factoryScope.fromTransaction( (session) -> {
			var n1 = new Node();
			n1.setDescription( "Parent" );
			var n1pk = new NodePk();
			n1pk.setLevel( 1 );
			n1pk.setName( "Root" );
			n1.setId( n1pk );

			var n2 = new Node();
			var n2pk = new NodePk();
			n2pk.setLevel( 2 );
			n2pk.setName( "Level 1: A" );
			n2.setParent( n1 );
			n2.setId( n2pk );

			session.persist( n2 );

			return n2pk;
		} );

		factoryScope.inTransaction( (session) -> {
			var n2 = session.find( Node.class, node2Pk );
			assertNotNull( n2 );
			assertNotNull( n2.getParent() );
			assertEquals( 1, n2.getParent().getId().getLevel() );
		} );
	}

	@Test
	public void testManyToOneNonPk(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var order = new Order();
			order.setOrderNbr( "123" );
			session.persist( order );

			var ol = new OrderLine();
			ol.setItem( "Mouse" );
			ol.setOrder( order );
			session.persist( ol );
		} );

		factoryScope.inTransaction( (session) -> {
			var ol = session.find( OrderLine.class, 1 );
			assertNotNull( ol.getOrder() );
			assertEquals( "123", ol.getOrder().getOrderNbr() );
			assertTrue( ol.getOrder().getOrderLines().contains( ol ) );
		} );
	}

	@Test
	public void testManyToOneNonPkSecondaryTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var order = new Order();
			order.setOrderNbr( "123" );
			session.persist( order );

			var ol = new OrderLine();
			ol.setItem( "Mouse" );
			ol.setReplacementOrder( order );
			session.persist( ol );
		} );

		factoryScope.inTransaction( (session) -> {
			var ol = session.find( OrderLine.class, 1 );
			assertNotNull( ol.getReplacementOrder() );
			assertEquals( "123", ol.getReplacementOrder().getOrderNbr() );
			assertFalse( ol.getReplacementOrder().getOrderLines().contains( ol ) );
		} );
	}

	@Test
	public void testTwoManyToOneNonPk(SessionFactoryScope factoryScope) {
		//2 many-to-one non pk pointing to the same referencedColumnName should not fail
		factoryScope.inTransaction( (session) -> {
			var customer = new org.hibernate.orm.test.annotations.manytoone.Customer();
			customer.userId="123";
			var customer2 = new org.hibernate.orm.test.annotations.manytoone.Customer();
			customer2.userId="124";
			session.persist( customer2 );
			session.persist( customer );
			var deal = new Deal();
			deal.from = customer;
			deal.to = customer2;
			session.persist( deal );
		} );

		factoryScope.inTransaction( (session) -> {
			var deal = session.find( Deal.class, 1 );
			assertNotNull( deal.from );
			assertNotNull( deal.to );
		} );
	}

	@Test
	public void testFormulaOnOtherSide(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var frame = new Frame();
			frame.setName( "Prada" );
			session.persist( frame );

			var l = new Lens();
			l.setFocal( 2.5f );
			l.setFrame( frame );
			session.persist( l );

			var r = new Lens();
			r.setFocal( 1.2f);
			r.setFrame( frame );
			session.persist( r );
		} );

		factoryScope.inTransaction( (session) -> {
			var frame = session.find( Frame.class, 1L );
			assertEquals( 2, frame.getLenses().size() );
			assertTrue( frame.getLenses().iterator().next().getLength() <= 1 / 1.2f );
			assertTrue( frame.getLenses().iterator().next().getLength() >= 1 / 2.5f );
		} );
	}
}
